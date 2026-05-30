package com.liminghan.campusai.vo;

import lombok.Data;

import java.util.List;

@Data
public class ChatResponseVO {

    private String answer;

    private String sourceType;

    private Long messageId;

    private Long conversationId;

    private String promptPreview;

    /** LLM 模式: mock / real / spring-ai */
    private String llmMode;

    /** 检索耗时（毫秒） */
    private long retrievalTimeMs;

    /** 模型生成耗时（毫秒） */
    private long generationTimeMs;

    /** 命中文档片段列表（含得分和来源信息） */
    private List<MatchedChunkVO> matchedChunks;
}
