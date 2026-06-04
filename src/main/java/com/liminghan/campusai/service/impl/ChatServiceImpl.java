package com.liminghan.campusai.service.impl;

import com.liminghan.campusai.dto.ChatAskRequest;
import com.liminghan.campusai.entity.ChatRecord;
import com.liminghan.campusai.entity.Conversation;
import com.liminghan.campusai.entity.KbDocumentChunk;
import com.liminghan.campusai.service.*;
import com.liminghan.campusai.util.PromptBuilder;
import com.liminghan.campusai.vo.ChatResponseVO;
import com.liminghan.campusai.vo.MatchedChunkVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    private final QuestionRouter questionRouter;
    private final RagService ragService;
    private final AcademicService academicService;
    private final ChatRecordService chatRecordService;
    private final ConversationService conversationService;
    private final PromptBuilder promptBuilder;
    private final MockLlmClient mockLlmClient;
    private final DeepSeekLlmClient deepSeekLlmClient;
    private final SpringAiChatClientLlmClient springAiChatClientLlmClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${llm.mode:mock}")
    private String llmMode;

    @Value("${app.cache.faq-ttl-minutes:30}")
    private long faqTtlMinutes;

    public ChatServiceImpl(QuestionRouter questionRouter,
                           RagService ragService,
                           AcademicService academicService,
                           ChatRecordService chatRecordService,
                           ConversationService conversationService,
                           PromptBuilder promptBuilder,
                           MockLlmClient mockLlmClient,
                           DeepSeekLlmClient deepSeekLlmClient,
                           SpringAiChatClientLlmClient springAiChatClientLlmClient,
                           RedisTemplate<String, Object> redisTemplate) {
        this.questionRouter = questionRouter;
        this.ragService = ragService;
        this.academicService = academicService;
        this.chatRecordService = chatRecordService;
        this.conversationService = conversationService;
        this.promptBuilder = promptBuilder;
        this.mockLlmClient = mockLlmClient;
        this.deepSeekLlmClient = deepSeekLlmClient;
        this.springAiChatClientLlmClient = springAiChatClientLlmClient;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Transactional
    public ChatResponseVO ask(ChatAskRequest request) {
        QuestionType questionType = questionRouter.route(request.getQuestion());
        List<MatchedChunkVO> matchedChunks = List.of();
        String answer;
        String promptPreview = "";
        long retrievalTimeMs = 0;
        long generationTimeMs = 0;

        Long conversationId = ensureConversation(request);
        String history = loadConversationHistory(conversationId);

        String faqKey = "chat:faq:" + Integer.toHexString(Objects.hash(request.getKnowledgeBaseId(), request.getQuestion()));
        String cachedAnswer = readStringCache(faqKey);

        if (cachedAnswer != null) {
            answer = cachedAnswer;
        } else if (questionType == QuestionType.ACADEMIC_QUERY) {
            answer = academicService.answerAcademicQuestion(request.getQuestion());
        } else {
            List<KbDocumentChunk> rawChunks = List.of();
            boolean hasSelectedKnowledgeBase = request.getKnowledgeBaseId() != null && request.getKnowledgeBaseId() > 0;

            if (hasSelectedKnowledgeBase) {
                long retrievalStart = System.currentTimeMillis();
                try {
                    rawChunks = ragService.retrieveTopK(request.getKnowledgeBaseId(), request.getQuestion(), 5);
                    matchedChunks = ragService.retrieveTopKWithScore(request.getKnowledgeBaseId(), request.getQuestion(), 5);
                } catch (Exception e) {
                    log.warn("RAG retrieval failed: {}", e.getMessage());
                }
                retrievalTimeMs = System.currentTimeMillis() - retrievalStart;
            }

            String prompt;
            if (hasSelectedKnowledgeBase && rawChunks.isEmpty()) {
                prompt = "知识库命中不足，未调用 LLM。问题：" + request.getQuestion();
                answer = buildInsufficientKnowledgeAnswer(request.getQuestion());
            } else if (!rawChunks.isEmpty()) {
                prompt = promptBuilder.buildRagPrompt(request.getQuestion(), rawChunks, history);
                long generationStart = System.currentTimeMillis();
                answer = chooseClient().generate(prompt);
                generationTimeMs = System.currentTimeMillis() - generationStart;
            } else {
                prompt = promptBuilder.buildGeneralPrompt(request.getQuestion(), history);
                long generationStart = System.currentTimeMillis();
                answer = chooseClient().generate(prompt);
                generationTimeMs = System.currentTimeMillis() - generationStart;
            }

            promptPreview = prompt.length() > 500 ? prompt.substring(0, 500) + "..." : prompt;

            if (hasSelectedKnowledgeBase && !matchedChunks.isEmpty()) {
                writeValueCache(faqKey, answer, Duration.ofMinutes(faqTtlMinutes));
            }
        }

        ChatRecord record = saveChatRecord(request, conversationId, questionType, matchedChunks,
                answer, promptPreview, retrievalTimeMs, generationTimeMs);

        return buildResponse(record, conversationId, questionType, matchedChunks, answer, promptPreview,
                retrievalTimeMs, generationTimeMs);
    }

    private Long ensureConversation(ChatAskRequest request) {
        if (request.getConversationId() != null && request.getConversationId() > 0) {
            Conversation conv = conversationService.getById(request.getConversationId());
            if (conv != null) {
                conv.setUpdatedAt(LocalDateTime.now());
                conversationService.updateById(conv);
                return conv.getId();
            }
        }

        Conversation conv = new Conversation();
        conv.setUserId(request.getUserId() != null ? request.getUserId() : 1L);
        conv.setKnowledgeBaseId(request.getKnowledgeBaseId());
        String title = request.getQuestion();
        if (title != null && title.length() > 30) {
            title = title.substring(0, 30) + "...";
        }
        conv.setTitle(title != null ? title : "新对话");
        conv.setCreatedAt(LocalDateTime.now());
        conv.setUpdatedAt(LocalDateTime.now());
        conversationService.save(conv);
        return conv.getId();
    }

    private String loadConversationHistory(Long conversationId) {
        try {
            String key = "chat:conv:" + conversationId;
            List<Object> list = redisTemplate.opsForList().range(key, 0, -1);
            if (list == null || list.isEmpty()) {
                List<ChatRecord> records = chatRecordService.lambdaQuery()
                        .eq(ChatRecord::getConversationId, conversationId)
                        .orderByDesc(ChatRecord::getCreatedAt)
                        .last("limit 20")
                        .list();
                if (records.isEmpty()) {
                    return null;
                }
                Collections.reverse(records);
                return records.stream()
                        .map(r -> "用户: " + r.getQuestion() + "\n助手: " + r.getAnswer())
                        .collect(Collectors.joining("\n"));
            }
            return String.join("\n", list.stream().map(Object::toString).toList());
        } catch (Exception e) {
            return null;
        }
    }

    private LlmClient chooseClient() {
        if ("real".equalsIgnoreCase(llmMode)) return deepSeekLlmClient;
        if ("spring-ai".equalsIgnoreCase(llmMode)) return springAiChatClientLlmClient;
        return mockLlmClient;
    }

    private String buildInsufficientKnowledgeAnswer(String question) {
        return "当前知识库资料不足，暂时无法基于可信课程资料回答这个问题。\n\n"
                + "你可以尝试：\n"
                + "1. 切换到更相关的课程知识库；\n"
                + "2. 使用更具体的关键词重新提问；\n"
                + "3. 请教师补充相关讲义、实验指导或复习资料。\n\n"
                + "原问题：" + question;
    }

    private ChatRecord saveChatRecord(ChatAskRequest request, Long conversationId,
                                      QuestionType questionType, List<MatchedChunkVO> chunks,
                                      String answer, String promptPreview,
                                      long retrievalTimeMs, long generationTimeMs) {
        ChatRecord record = new ChatRecord();
        record.setUserId(request.getUserId() != null ? request.getUserId() : 1L);
        record.setUsername(request.getUsername() != null ? request.getUsername() : "");
        record.setConversationId(conversationId);
        record.setKnowledgeBaseId(request.getKnowledgeBaseId() != null ? request.getKnowledgeBaseId() : 0L);
        record.setQuestion(request.getQuestion());
        record.setAnswer(answer);
        record.setSourceType(questionType.name());
        record.setMatchedChunkIds(chunks.stream().map(c -> String.valueOf(c.getId())).collect(Collectors.joining(",")));
        record.setPromptPreview(promptPreview);
        record.setLlmMode(llmMode);
        record.setRetrievalTimeMs(retrievalTimeMs);
        record.setGenerationTimeMs(generationTimeMs);
        record.setCreatedAt(LocalDateTime.now());
        chatRecordService.save(record);

        appendConversationHistory(conversationId, request.getQuestion(), answer);
        return record;
    }

    private void appendConversationHistory(Long conversationId, String question, String answer) {
        try {
            String key = "chat:conv:" + conversationId;
            redisTemplate.opsForList().rightPush(key, "用户: " + question);
            redisTemplate.opsForList().rightPush(key, "助手: " + answer);
            redisTemplate.opsForList().trim(key, -20, -1);
            redisTemplate.expire(key, Duration.ofHours(24));
        } catch (Exception ignored) {
        }
    }

    private ChatResponseVO buildResponse(ChatRecord record, Long conversationId, QuestionType questionType,
                                         List<MatchedChunkVO> matchedChunks, String answer,
                                         String promptPreview, long retrievalTimeMs, long generationTimeMs) {
        ChatResponseVO response = new ChatResponseVO();
        response.setAnswer(answer);
        response.setSourceType(questionType.name());
        response.setMessageId(record.getId());
        response.setConversationId(conversationId);
        response.setPromptPreview(promptPreview);
        response.setLlmMode(llmMode);
        response.setRetrievalTimeMs(retrievalTimeMs);
        response.setGenerationTimeMs(generationTimeMs);
        response.setMatchedChunks(matchedChunks);
        return response;
    }

    private String readStringCache(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value == null ? null : value.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeValueCache(String key, String value, Duration timeout) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout);
        } catch (Exception ignored) {
        }
    }
}

