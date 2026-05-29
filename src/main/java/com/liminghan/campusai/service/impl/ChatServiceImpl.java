package com.liminghan.campusai.service.impl;

import com.liminghan.campusai.dto.ChatAskRequest;
import com.liminghan.campusai.entity.ChatRecord;
import com.liminghan.campusai.entity.KbDocumentChunk;
import com.liminghan.campusai.service.AcademicService;
import com.liminghan.campusai.service.ChatRecordService;
import com.liminghan.campusai.service.ChatService;
import com.liminghan.campusai.service.QuestionRouter;
import com.liminghan.campusai.service.QuestionType;
import com.liminghan.campusai.service.RagService;
import com.liminghan.campusai.util.PromptBuilder;
import com.liminghan.campusai.vo.ChatResponseVO;
import com.liminghan.campusai.vo.MatchedChunkVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private final QuestionRouter questionRouter;
    private final RagService ragService;
    private final AcademicService academicService;
    private final ChatRecordService chatRecordService;
    private final PromptBuilder promptBuilder;
    private final MockLlmClient mockLlmClient;
    private final DeepSeekLlmClient deepSeekLlmClient;
    private final SpringAiChatClientLlmClient springAiChatClientLlmClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${llm.mode:mock}")
    private String llmMode;

    @Value("${app.cache.faq-ttl-minutes:30}")
    private long faqTtlMinutes;

    @Value("${app.cache.context-ttl-hours:2}")
    private long contextTtlHours;

    public ChatServiceImpl(QuestionRouter questionRouter,
                           RagService ragService,
                           AcademicService academicService,
                           ChatRecordService chatRecordService,
                           PromptBuilder promptBuilder,
                           MockLlmClient mockLlmClient,
                           DeepSeekLlmClient deepSeekLlmClient,
                           SpringAiChatClientLlmClient springAiChatClientLlmClient,
                           RedisTemplate<String, Object> redisTemplate) {
        this.questionRouter = questionRouter;
        this.ragService = ragService;
        this.academicService = academicService;
        this.chatRecordService = chatRecordService;
        this.promptBuilder = promptBuilder;
        this.mockLlmClient = mockLlmClient;
        this.deepSeekLlmClient = deepSeekLlmClient;
        this.springAiChatClientLlmClient = springAiChatClientLlmClient;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ChatResponseVO ask(ChatAskRequest request) {
        QuestionType questionType = questionRouter.route(request.getQuestion());
        List<KbDocumentChunk> matchedChunks = List.of();
        String answer;
        String promptPreview = "";

        String faqKey = "chat:faq:" + Integer.toHexString(Objects.hash(request.getKnowledgeBaseId(), request.getQuestion()));
        String cachedAnswer = readStringCache(faqKey);
        if (cachedAnswer != null) {
            answer = cachedAnswer;
        } else if (questionType == QuestionType.ACADEMIC_QUERY) {
            answer = academicService.answerAcademicQuestion(request.getQuestion());
        } else if (questionType == QuestionType.RAG) {
            matchedChunks = ragService.retrieveTopK(request.getKnowledgeBaseId(), request.getQuestion(), 3);
            String prompt = promptBuilder.buildRagPrompt(request.getQuestion(), matchedChunks);
            promptPreview = prompt;
            answer = chooseClient().generate(prompt);
            writeValueCache(faqKey, answer, Duration.ofMinutes(faqTtlMinutes));
        } else {
            String prompt = promptBuilder.buildGeneralPrompt(request.getQuestion());
            promptPreview = prompt;
            answer = chooseClient().generate(prompt);
        }

        ChatRecord record = saveChatRecord(request, questionType, matchedChunks, answer);
        appendContext(request.getUserId(), request.getQuestion(), answer);
        return buildResponse(record, questionType, matchedChunks, answer, promptPreview);
    }

    private com.liminghan.campusai.service.LlmClient chooseClient() {
        if ("real".equalsIgnoreCase(llmMode)) {
            return deepSeekLlmClient;
        }
        if ("spring-ai".equalsIgnoreCase(llmMode)) {
            return springAiChatClientLlmClient;
        }
        return mockLlmClient;
    }

    private ChatRecord saveChatRecord(ChatAskRequest request, QuestionType questionType, List<KbDocumentChunk> chunks, String answer) {
        ChatRecord record = new ChatRecord();
        record.setUserId(request.getUserId());
        record.setQuestion(request.getQuestion());
        record.setAnswer(answer);
        record.setSourceType(questionType.name());
        record.setMatchedChunkIds(chunks.stream()
                .map(KbDocumentChunk::getId)
                .map(String::valueOf)
                .collect(Collectors.joining(",")));
        record.setCreatedAt(LocalDateTime.now());
        chatRecordService.save(record);
        return record;
    }

    private ChatResponseVO buildResponse(ChatRecord record, QuestionType questionType, List<KbDocumentChunk> chunks, String answer, String promptPreview) {
        ChatResponseVO response = new ChatResponseVO();
        response.setAnswer(answer);
        response.setSourceType(questionType.name());
        response.setConversationId(record.getId());
        response.setPromptPreview(promptPreview);
        response.setMatchedChunks(chunks.stream().map(this::toMatchedChunkVO).toList());
        return response;
    }

    private MatchedChunkVO toMatchedChunkVO(KbDocumentChunk chunk) {
        MatchedChunkVO vo = new MatchedChunkVO();
        vo.setId(chunk.getId());
        vo.setChunkIndex(chunk.getChunkIndex());
        vo.setContent(chunk.getContent());
        return vo;
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
            // Redis is an optimization here; chat should still work when Redis is unavailable.
        }
    }

    private void appendContext(Long userId, String question, String answer) {
        try {
            String key = "chat:context:" + userId;
            redisTemplate.opsForList().rightPush(key, "Q: " + question + "\nA: " + answer);
            redisTemplate.opsForList().trim(key, -5, -1);
            redisTemplate.expire(key, Duration.ofHours(contextTtlHours));
        } catch (Exception ignored) {
            // Redis context cache must not break the core chat flow.
        }
    }
}
