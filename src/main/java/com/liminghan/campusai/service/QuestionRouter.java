package com.liminghan.campusai.service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuestionRouter {

    private static final List<String> ACADEMIC_KEYWORDS = List.of("成绩", "分数", "课程", "平均分", "绩点");
    private static final List<String> RAG_KEYWORDS = List.of("复习", "知识点", "解释", "资料", "怎么学");

    public QuestionType route(String question) {
        String safeQuestion = question == null ? "" : question;
        if (containsAny(safeQuestion, ACADEMIC_KEYWORDS)) {
            return QuestionType.ACADEMIC_QUERY;
        }
        if (containsAny(safeQuestion, RAG_KEYWORDS)) {
            return QuestionType.RAG;
        }
        return QuestionType.GENERAL_CHAT;
    }

    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }
}
