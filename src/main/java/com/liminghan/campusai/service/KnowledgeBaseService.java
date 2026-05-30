package com.liminghan.campusai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liminghan.campusai.dto.DocumentCreateRequest;
import com.liminghan.campusai.dto.KnowledgeBaseCreateRequest;
import com.liminghan.campusai.entity.KbDocument;
import com.liminghan.campusai.entity.KbDocumentChunk;
import com.liminghan.campusai.entity.KnowledgeBase;

import java.util.List;

public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    KnowledgeBase createKnowledgeBase(KnowledgeBaseCreateRequest request);

    List<KnowledgeBase> listKnowledgeBases();

    KnowledgeBase getKnowledgeBase(Long id);

    void deleteKnowledgeBase(Long id);

    Long addDocument(Long knowledgeBaseId, DocumentCreateRequest request);

    Long uploadDocumentAsync(Long knowledgeBaseId, String fileName, String fileType, Long fileSize, String content);

    void processDocumentChunks(Long documentId);

    List<KbDocumentChunk> listChunks(Long knowledgeBaseId);

    List<KbDocument> listDocuments(Long knowledgeBaseId);

    List<KbDocumentChunk> listDocumentChunks(Long documentId);

    void reprocessDocument(Long documentId);

    void deleteDocument(Long documentId);
}
