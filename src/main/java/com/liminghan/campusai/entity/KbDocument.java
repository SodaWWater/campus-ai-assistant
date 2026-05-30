package com.liminghan.campusai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kb_document")
public class KbDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long knowledgeBaseId;

    private String title;

    private String content;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String status;

    private String errorMessage;

    private Integer chunkCount;

    private Long uploadedBy;

    private LocalDateTime processedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
