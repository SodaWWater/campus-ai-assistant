package com.liminghan.campusai.service.impl;

import com.liminghan.campusai.service.LlmClient;
import org.springframework.stereotype.Component;

@Component
public class MockLlmClient implements LlmClient {

    @Override
    public String generate(String prompt) {
        return "这是 mock 模式下的模拟回答：系统已完成知识库检索、Prompt 构建和 LLM Client 调用流程。\n\n"
                + "Prompt 摘要：" + summarize(prompt);
    }

    private String summarize(String prompt) {
        String safePrompt = prompt == null ? "" : prompt.replaceAll("\\s+", " ").trim();
        if (safePrompt.length() <= 160) {
            return safePrompt;
        }
        return safePrompt.substring(0, 160) + "...";
    }
}

