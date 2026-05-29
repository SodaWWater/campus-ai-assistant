package com.liminghan.campusai.service.impl;

import com.liminghan.campusai.common.BusinessException;
import com.liminghan.campusai.common.ErrorCode;
import com.liminghan.campusai.service.LlmClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
public class SpringAiChatClientLlmClient implements LlmClient {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;

    public SpringAiChatClientLlmClient(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
    }

    @Override
    public String generate(String prompt) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Spring AI ChatClient.Builder is not available");
        }
        return builder.build()
                .prompt()
                .user(prompt)
                .call()
                .content();
    }
}
