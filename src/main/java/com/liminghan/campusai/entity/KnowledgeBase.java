package com.liminghan.campusai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kb_knowledge_base")
public class KnowledgeBase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private Long ownerId;

    private String ownerName;

    private String visibility;

    private Integer documentCount;

    private Integer chunkCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
