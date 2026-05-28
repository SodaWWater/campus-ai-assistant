package com.liminghan.campusai.service.impl;

import com.liminghan.campusai.service.LlmClient;
import org.springframework.stereotype.Component;

@Component
public class MockLlmClient implements LlmClient {

    @Override
    public String generate(String prompt) {
        return "根据知识库片段生成的模拟回答：当前为 mock 模式，已完成 Prompt 构建和 LLM Client 调用流程。问题上下文摘要："
                + summarize(prompt);
    }

    private String summarize(String prompt) {
        String safePrompt = prompt == null ? "" : prompt.replaceAll("\\s+", " ").trim();
        if (safePrompt.length() <= 120) {
            return safePrompt;
        }
        return safePrompt.substring(0, 120) + "...";
    }
}
