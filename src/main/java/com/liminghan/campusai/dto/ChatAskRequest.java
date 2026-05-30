package com.liminghan.campusai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatAskRequest {

    private Long userId;

    private String username;

    /** 对话 ID，为空时自动创建新对话 */
    private Long conversationId;

    @NotBlank(message = "question cannot be blank")
    private String question;

    /** 知识库 ID，可选。非空时系统会检索 RAG 资料辅助回答 */
    private Long knowledgeBaseId;
}
