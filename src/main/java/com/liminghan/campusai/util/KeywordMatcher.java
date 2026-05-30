package com.liminghan.campusai.util;

import com.liminghan.campusai.entity.KbDocumentChunk;
import com.liminghan.campusai.vo.MatchedChunkVO;
import org.springframework.stereotype.Component;

import java.util.*;
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

    public List<MatchedChunkVO> topKWithScore(String question, List<KbDocumentChunk> chunks, int topK,
                                               Map<Long, String> titleMap) {
        Set<String> questionTokens = tokenize(question);
        return chunks.stream()
                .map(chunk -> {
                    int s = score(questionTokens, chunk.getContent());
                    MatchedChunkVO vo = new MatchedChunkVO();
                    vo.setId(chunk.getId());
                    vo.setChunkIndex(chunk.getChunkIndex());
                    vo.setContent(chunk.getContent());
                    vo.setDocumentTitle(titleMap.getOrDefault(chunk.getDocumentId(), "未知文档"));
                    vo.setScore(s);
                    return vo;
                })
                .sorted(Comparator.comparingInt(MatchedChunkVO::getScore).reversed())
                .limit(topK)
                .filter(vo -> vo.getScore() > 0)
                .toList();
    }

    public String extractKeywords(String text) {
        return String.join(",", tokenize(text));
    }

    public int score(Set<String> questionTokens, String content) {
        Set<String> contentTokens = tokenize(content);
        int s = 0;
        for (String token : questionTokens) {
            if (contentTokens.contains(token)) {
                s++;
            }
        }
        return s;
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
