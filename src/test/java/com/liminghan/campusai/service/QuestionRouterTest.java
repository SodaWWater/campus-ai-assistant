package com.liminghan.campusai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionRouterTest {

    private final QuestionRouter router = new QuestionRouter();

    @Test
    @DisplayName("should route grade-related questions to ACADEMIC_QUERY")
    void shouldRouteAcademicQuery() {
        assertThat(router.route("我的数据库成绩怎么样？")).isEqualTo(QuestionType.ACADEMIC_QUERY);
        assertThat(router.route("高等数学分数是多少")).isEqualTo(QuestionType.ACADEMIC_QUERY);
    }

    @Test
    @DisplayName("should route knowledge-related questions to RAG")
    void shouldRouteRagQuery() {
        assertThat(router.route("请解释ArrayList的底层实现原理")).isEqualTo(QuestionType.RAG);
        assertThat(router.route("怎么学习数据结构？")).isEqualTo(QuestionType.RAG);
    }

    @Test
    @DisplayName("should route general questions to GENERAL_CHAT")
    void shouldRouteGeneralChat() {
        assertThat(router.route("你好")).isEqualTo(QuestionType.GENERAL_CHAT);
        assertThat(router.route("今天天气怎么样？")).isEqualTo(QuestionType.GENERAL_CHAT);
    }

    @Test
    @DisplayName("should prioritize academic keywords over RAG keywords")
    void shouldPrioritizeAcademic() {
        // "成绩" (academic) should take priority over "资料" (RAG)
        assertThat(router.route("我的期末成绩怎么对照资料分析？")).isEqualTo(QuestionType.ACADEMIC_QUERY);
    }

    @Test
    @DisplayName("should handle null question")
    void shouldHandleNull() {
        assertThat(router.route(null)).isEqualTo(QuestionType.GENERAL_CHAT);
    }

    @Test
    @DisplayName("should handle empty question")
    void shouldHandleEmpty() {
        assertThat(router.route("")).isEqualTo(QuestionType.GENERAL_CHAT);
    }
}
