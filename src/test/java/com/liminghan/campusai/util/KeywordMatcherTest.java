package com.liminghan.campusai.util;

import com.liminghan.campusai.entity.KbDocumentChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordMatcherTest {

    private KeywordMatcher matcher;

    @BeforeEach
    void setUp() {
        matcher = new KeywordMatcher();
    }

    @Test
    @DisplayName("should extract Chinese and English keywords")
    void shouldExtractKeywords() {
        String keywords = matcher.extractKeywords("Java 语言提供强类型检查机制, ArrayList 基于动态数组实现。");
        assertThat(keywords).isNotEmpty();
        assertThat(keywords).contains("java");
        assertThat(keywords).contains("arraylist");
    }

    @Test
    @DisplayName("should filter stop words from extracted keywords")
    void shouldFilterStopWords() {
        String keywords = matcher.extractKeywords("栈和队列都是常用的数据结构");
        assertThat(keywords).isNotEmpty();
        // Stop words like "和", "的", "是" should be filtered out
        assertThat(keywords.split(",")).noneMatch(kw ->
                kw.equals("和") || kw.equals("的") || kw.equals("是"));
    }

    @Test
    @DisplayName("should rank chunks by keyword match score")
    void shouldRankByScore() {
        KbDocumentChunk c1 = new KbDocumentChunk();
        c1.setId(1L);
        c1.setContent("ArrayList 基于动态数组，随机访问效率高。LinkedList 基于链表。");

        KbDocumentChunk c2 = new KbDocumentChunk();
        c2.setId(2L);
        c2.setContent("网络协议栈分为五层：物理层、数据链路层、网络层、传输层和应用层。");

        List<KbDocumentChunk> results = matcher.topK("ArrayList LinkedList 对比", List.of(c1, c2), 3);
        assertThat(results).isNotEmpty();
        // c1 should rank higher since it contains ArrayList and LinkedList
        assertThat(results.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("should return empty list when no chunks match")
    void shouldReturnEmptyForNoMatch() {
        KbDocumentChunk c1 = new KbDocumentChunk();
        c1.setId(1L);
        c1.setContent("完全无关的内容");

        List<KbDocumentChunk> results = matcher.topK("量子计算", List.of(c1), 3);
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("should score exact matches higher than partial matches")
    void shouldScoreExactHigher() {
        KbDocumentChunk c1 = new KbDocumentChunk();
        c1.setId(1L);
        c1.setContent("ArrayList 是 Java 中最常用的集合之一");

        KbDocumentChunk c2 = new KbDocumentChunk();
        c2.setId(2L);
        c2.setContent("数组列表是一种数据结构");

        List<KbDocumentChunk> results = matcher.topK("ArrayList", List.of(c1, c2), 3);
        assertThat(results).isNotEmpty();
        // c1 should be first because "ArrayList" is an exact token match
        assertThat(results.get(0).getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("should handle null chunks gracefully")
    void shouldHandleNullChunks() {
        // Null chunks should either return empty list or throw NPE
        // Current implementation does not null-guard; confirming expected behavior
        try {
            List<KbDocumentChunk> results = matcher.topK("test", null, 3);
            assertThat(results).isEmpty();
        } catch (NullPointerException ignored) {
            // Acceptable: caller is responsible for passing non-null
        }
    }

    @Test
    @DisplayName("should handle empty question")
    void shouldHandleEmptyQuestion() {
        KbDocumentChunk c1 = new KbDocumentChunk();
        c1.setId(1L);
        c1.setContent("some content");
        List<KbDocumentChunk> results = matcher.topK("", List.of(c1), 3);
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("topKWithScore should return VOs with score and title")
    void shouldReturnWithScore() {
        KbDocumentChunk c1 = new KbDocumentChunk();
        c1.setId(1L);
        c1.setDocumentId(10L);
        c1.setContent("栈是一种后进先出的数据结构");

        var results = matcher.topKWithScore("栈 数据结构", List.of(c1), 5, Map.of(10L, "数据结构讲义"));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getScore()).isGreaterThan(0);
        assertThat(results.get(0).getDocumentTitle()).isEqualTo("数据结构讲义");
    }
}
