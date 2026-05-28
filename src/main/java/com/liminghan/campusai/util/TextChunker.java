package com.liminghan.campusai.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private static final int CHUNK_SIZE = 400;
    private static final int MIN_CHUNK_SIZE = 300;

    public List<String> split(String text) {
        String normalized = text == null ? "" : text.trim();
        List<String> chunks = new ArrayList<>();
        if (normalized.isEmpty()) {
            return chunks;
        }

        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + CHUNK_SIZE, normalized.length());
            if (end < normalized.length() && end - start >= MIN_CHUNK_SIZE) {
                int punctuation = findLastPunctuation(normalized, start, end);
                if (punctuation > start + MIN_CHUNK_SIZE) {
                    end = punctuation + 1;
                }
            }
            chunks.add(normalized.substring(start, end).trim());
            start = end;
        }
        return chunks;
    }

    private int findLastPunctuation(String text, int start, int end) {
        for (int i = end - 1; i >= start; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '.' || c == '!' || c == '?') {
                return i;
            }
        }
        return -1;
    }
}
