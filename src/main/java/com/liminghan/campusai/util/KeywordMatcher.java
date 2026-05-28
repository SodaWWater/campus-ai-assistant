package com.liminghan.campusai.util;

import com.liminghan.campusai.entity.KbDocumentChunk;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class KeywordMatcher {

    public List<KbDocumentChunk> topK(String question, List<KbDocumentChunk> chunks, int topK) {
        Set<String> questionTokens = tokenize(question);
        return chunks.stream()
                .sorted(Comparator.comparingInt((KbDocumentChunk chunk) -> score(questionTokens, chunk.getContent())).reversed())
                .limit(topK)
                .toList();
    }

    public String extractKeywords(String text) {
        return String.join(",", tokenize(text));
    }

    private int score(Set<String> questionTokens, String content) {
        Set<String> contentTokens = tokenize(content);
        int score = 0;
        for (String token : questionTokens) {
            if (contentTokens.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private Set<String> tokenize(String text) {
        String safeText = text == null ? "" : text.toLowerCase();
        return safeText.chars()
                .mapToObj(ch -> String.valueOf((char) ch))
                .filter(token -> !token.isBlank())
                .filter(token -> !",.，。！？!?;；:：()（）[]【】\"' \r\n\t".contains(token))
                .collect(Collectors.toSet());
    }
}
