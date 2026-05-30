package com.liminghan.campusai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_conversation")
public class Conversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private Long knowledgeBaseId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
