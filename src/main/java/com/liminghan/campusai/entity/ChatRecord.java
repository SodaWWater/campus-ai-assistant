package com.liminghan.campusai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_record")
public class ChatRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    private Long conversationId;

    private Long knowledgeBaseId;

    private String question;

    private String answer;

    private String sourceType;

    private String matchedChunkIds;

    private String promptPreview;

    private String llmMode;

    private Long retrievalTimeMs;

    private Long generationTimeMs;

    private LocalDateTime createdAt;
}
