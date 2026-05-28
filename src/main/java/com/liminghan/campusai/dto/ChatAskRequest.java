package com.liminghan.campusai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatAskRequest {

    @NotNull(message = "userId cannot be null")
    private Long userId;

    @NotBlank(message = "question cannot be blank")
    private String question;

    private Long knowledgeBaseId;
}
