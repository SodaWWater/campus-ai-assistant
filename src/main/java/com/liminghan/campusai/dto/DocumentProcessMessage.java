package com.liminghan.campusai.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DocumentProcessMessage implements Serializable {

    private Long documentId;

    private Long knowledgeBaseId;
}
