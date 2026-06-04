package com.liminghan.campusai.service.impl;

import com.liminghan.campusai.service.LlmClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Token-aware conversation history manager.
 * <p>
 * Two layers of protection against context-window overflow:
 * <ol>
 *   <li>Token-budget truncation — trims oldest turns until within budget.</li>
 *   <li>Compression — when turns exceed a threshold, summarizes oldest turns via LLM.</li>
 * </ol>
 */
@Component
public class ConversationCompressor {

    private static final Logger log = LoggerFactory.getLogger(ConversationCompressor.class);

    private final LlmClient llmClient;
    private final int maxHistoryTokens;
    private final int compressAfterTurns;
    private final String llmMode;

    public ConversationCompressor(
            @Value("${app.chat.max-history-tokens:3000}") int maxHistoryTokens,
            @Value("${app.chat.compress-after-turns:8}") int compressAfterTurns,
            @Value("${llm.mode:mock}") String llmMode,
            DeepSeekLlmClient deepSeekLlmClient,
            SpringAiChatClientLlmClient springAiChatClientLlmClient,
            MockLlmClient mockLlmClient) {
        this.maxHistoryTokens = maxHistoryTokens;
        this.compressAfterTurns = compressAfterTurns;
        this.llmMode = llmMode;
        // Choose the right client based on mode (same logic as ChatServiceImpl.chooseClient)
        if ("real".equalsIgnoreCase(llmMode)) {
            this.llmClient = deepSeekLlmClient;
        } else if ("spring-ai".equalsIgnoreCase(llmMode)) {
            this.llmClient = springAiChatClientLlmClient;
        } else {
            this.llmClient = mockLlmClient;
        }
    }

    /**
     * Process conversation history: compress if too long, then truncate to token budget.
     *
     * @param history full conversation history text (format: "用户: ...\n助手: ...")
     * @param turnCount number of Q&A pairs in this conversation
     * @return processed history that fits within the token budget
     */
    public String process(String history, int turnCount) {
        if (history == null || history.isBlank()) {
            return null;
        }

        String result = history;

        // Layer 2: Compress old turns when conversation gets too long
        if (turnCount >= compressAfterTurns) {
            result = compressOldTurns(result, turnCount);
        }

        // Layer 1: Token-budget truncation
        if (estimateTokens(result) > maxHistoryTokens) {
            result = truncateToBudget(result, maxHistoryTokens);
        }

        return result;
    }

    /**
     * Summarize the oldest half of the conversation into a short paragraph,
     * keeping the most recent turns as-is.
     */
    private String compressOldTurns(String history, int turnCount) {
        // Split into individual Q&A blocks
        String[] lines = history.split("\n");
        if (lines.length < 4) {
            return history;
        }

        // Keep the last 4 turns (8 lines: user + assistant for each) as-is
        int keepLines = 8; // 4 turns = 8 lines
        if (lines.length <= keepLines) {
            return history;
        }

        StringBuilder oldPart = new StringBuilder();
        for (int i = 0; i < lines.length - keepLines; i++) {
            oldPart.append(lines[i]).append("\n");
        }

        // Compress only in real/spring-ai mode; in mock mode skip compression
        if (!"real".equalsIgnoreCase(llmMode) && !"spring-ai".equalsIgnoreCase(llmMode)) {
            // Mock mode: just keep recent turns, drop old ones
            StringBuilder recent = new StringBuilder();
            recent.append("[... 已自动总结更早的对话 ...]\n");
            for (int i = lines.length - keepLines; i < lines.length; i++) {
                recent.append(lines[i]).append("\n");
            }
            return recent.toString().trim();
        }

        // Use LLM to compress old conversation
        try {
            String summary = summarizeWithLlm(oldPart.toString().trim());
            StringBuilder compressed = new StringBuilder();
            compressed.append("[历史摘要] ").append(summary).append("\n\n");
            for (int i = lines.length - keepLines; i < lines.length; i++) {
                compressed.append(lines[i]).append("\n");
            }
            return compressed.toString().trim();
        } catch (Exception e) {
            log.warn("Conversation compression failed, keeping recent turns only. Cause: {}", e.getMessage());
            // Fallback: drop old turns
            StringBuilder recent = new StringBuilder();
            recent.append("[... 更早的对话已省略 ...]\n");
            for (int i = lines.length - keepLines; i < lines.length; i++) {
                recent.append(lines[i]).append("\n");
            }
            return recent.toString().trim();
        }
    }

    private String summarizeWithLlm(String oldConversation) {
        String prompt = """
                请用一段简短的中文摘要（不超过100字）总结以下对话的关键信息，
                只保留与课程知识和学习内容相关的要点：

                %s

                摘要：""".formatted(oldConversation);
        return llmClient.generate(prompt).trim();
    }

    /**
     * Truncate history from the oldest lines until the estimated token count
     * is within the budget.
     */
    String truncateToBudget(String history, int budget) {
        String[] lines = history.split("\n");
        if (lines.length <= 2) {
            return history;
        }

        StringBuilder result = new StringBuilder();
        int currentTokens = 0;

        // Build from newest to oldest, stop when budget exceeded
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i];
            int lineTokens = estimateTokens(line);
            if (currentTokens + lineTokens > budget && currentTokens > 0) {
                // Prepend truncation marker
                result.insert(0, "[... 更早的对话已省略 ...]\n");
                break;
            }
            result.insert(0, line + "\n");
            currentTokens += lineTokens;
        }

        return result.toString().trim();
    }

    /**
     * Rough token estimation.
     * Chinese: ~1.5 characters per token.
     * English/other: ~4 characters per token.
     */
    int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int chineseChars = 0;
        int otherChars = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                    || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A) {
                chineseChars++;
            } else if (!Character.isWhitespace(c)) {
                otherChars++;
            }
        }
        return (int) Math.ceil(chineseChars / 1.5 + otherChars / 4.0);
    }
}
