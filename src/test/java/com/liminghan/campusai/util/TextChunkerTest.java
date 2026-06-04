package com.liminghan.campusai.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    @Test
    @DisplayName("should return empty list for null input")
    void shouldHandleNull() {
        assertThat(chunker.split(null)).isEmpty();
    }

    @Test
    @DisplayName("should return empty list for blank input")
    void shouldHandleBlank() {
        assertThat(chunker.split("   ")).isEmpty();
    }

    @Test
    @DisplayName("should return single chunk for short text")
    void shouldHandleShortText() {
        String text = "这是一段很短的文本。";
        List<String> result = chunker.split(text);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(text);
    }

    @Test
    @DisplayName("should split long text at punctuation boundaries")
    void shouldSplitAtPunctuation() {
        StringBuilder sb = new StringBuilder();
        // Generate text long enough to exceed CHUNK_SIZE (400) forcing multiple chunks
        for (int i = 0; i < 30; i++) {
            sb.append("这是第").append(i).append("段测试文本，包含一些课程内容和详细说明材料。");
        }
        List<String> result = chunker.split(sb.toString());
        assertThat(result).hasSizeGreaterThan(1);
        // All chunks except possibly the last should have reasonable length
        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i).length()).isGreaterThan(50);
        }
    }

    @Test
    @DisplayName("should handle English text with periods")
    void shouldHandleEnglishText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("This is sentence number ").append(i)
              .append(". It contains some additional words for testing purposes. ");
        }
        List<String> result = chunker.split(sb.toString());
        assertThat(result).hasSizeGreaterThan(1);
    }

    @Test
    @DisplayName("should not create chunks smaller than MIN_CHUNK_SIZE except for short input")
    void shouldRespectMinChunkSize() {
        // Create text just over CHUNK_SIZE (400) but without punctuation in the first ~350 chars
        StringBuilder sb = new StringBuilder();
        sb.append("A".repeat(350));
        sb.append(".B".repeat(100));
        List<String> result = chunker.split(sb.toString());
        for (String chunk : result) {
            // Each chunk should be reasonable size
            assertThat(chunk.length()).isGreaterThanOrEqualTo(50);
        }
    }

    @Test
    @DisplayName("should handle mixed Chinese and English punctuation")
    void shouldHandleMixedPunctuation() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            sb.append("第").append(i).append("个段落包含中英文混合内容. English text here! ")
              .append("还有更多中文内容。以及一些解释。");
        }
        List<String> result = chunker.split(sb.toString());
        assertThat(result).hasSizeGreaterThan(1);
    }
}
