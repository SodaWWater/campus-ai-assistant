package com.liminghan.campusai.util;

import com.liminghan.campusai.entity.KbDocumentChunk;
import com.liminghan.campusai.vo.MatchedChunkVO;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class KeywordMatcher {

    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{IsHan}]{2,}|[a-zA-Z0-9_+#.-]{2,}");
    private static final Set<String> STOP_WORDS = Set.of(
            "什么", "怎么", "为什么", "如何", "一下", "这个", "那个", "以及", "或者", "如果", "请问",
            "the", "and", "for", "with", "from", "this", "that", "what", "how", "why"
    );

    public List<KbDocumentChunk> topK(String question, List<KbDocumentChunk> chunks, int topK) {
        Set<String> questionTokens = tokenize(question);
        return chunks.stream()
                .map(chunk -> Map.entry(chunk, score(questionTokens, chunk.getContent())))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<KbDocumentChunk, Integer>comparingByValue().reversed())
                .limit(topK)
                .map(Map.Entry::getKey)
                .toList();
    }

    public List<MatchedChunkVO> topKWithScore(String question, List<KbDocumentChunk> chunks, int topK,
                                               Map<Long, String> titleMap) {
        Set<String> questionTokens = tokenize(question);
        return chunks.stream()
                .map(chunk -> {
                    int s = score(questionTokens, chunk.getContent() + " " + nullToEmpty(chunk.getKeywords()));
                    MatchedChunkVO vo = new MatchedChunkVO();
                    vo.setId(chunk.getId());
                    vo.setChunkIndex(chunk.getChunkIndex());
                    vo.setContent(chunk.getContent());
                    vo.setDocumentTitle(titleMap.getOrDefault(chunk.getDocumentId(), "未知文档"));
                    vo.setScore(s);
                    return vo;
                })
                .filter(vo -> vo.getScore() > 0)
                .sorted(Comparator.comparingInt(MatchedChunkVO::getScore).reversed())
                .limit(topK)
                .toList();
    }

    public String extractKeywords(String text) {
        return tokenize(text).stream().limit(30).collect(Collectors.joining(","));
    }

    public int score(Set<String> questionTokens, String content) {
        if (questionTokens.isEmpty() || content == null || content.isBlank()) {
            return 0;
        }

        String normalizedContent = content.toLowerCase(Locale.ROOT);
        Set<String> contentTokens = tokenize(content);
        int score = 0;

        for (String token : questionTokens) {
            if (contentTokens.contains(token)) {
                score += token.length() >= 4 ? 4 : 2;
            } else if (normalizedContent.contains(token)) {
                score += 1;
            }
        }

        return score;
    }

    private Set<String> tokenize(String text) {
        String safeText = nullToEmpty(text).toLowerCase(Locale.ROOT);
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        Matcher matcher = WORD_PATTERN.matcher(safeText);
        while (matcher.find()) {
            String token = matcher.group().trim();
            if (!STOP_WORDS.contains(token)) {
                tokens.add(token);
                if (containsChinese(token)) {
                    tokens.addAll(chineseBigrams(token));
                }
            }
        }
        return tokens;
    }

    private List<String> chineseBigrams(String token) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < token.length() - 1; i++) {
            result.add(token.substring(i, i + 2));
        }
        return result;
    }

    private boolean containsChinese(String token) {
        return token.codePoints().anyMatch(code -> Character.UnicodeScript.of(code) == Character.UnicodeScript.HAN);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

