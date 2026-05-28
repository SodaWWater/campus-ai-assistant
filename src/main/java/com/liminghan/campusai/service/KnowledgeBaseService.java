package com.liminghan.campusai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.liminghan.campusai.dto.DocumentCreateRequest;
import com.liminghan.campusai.dto.KnowledgeBaseCreateRequest;
import com.liminghan.campusai.entity.KbDocumentChunk;
import com.liminghan.campusai.entity.KnowledgeBase;

import java.util.List;

public interface KnowledgeBaseService extends IService<KnowledgeBase> {

    KnowledgeBase createKnowledgeBase(KnowledgeBaseCreateRequest request);

    List<KnowledgeBase> listKnowledgeBases();

    KnowledgeBase getKnowledgeBase(Long id);

    void deleteKnowledgeBase(Long id);

    Long addDocument(Long knowledgeBaseId, DocumentCreateRequest request);

    List<KbDocumentChunk> listChunks(Long knowledgeBaseId);

    void deleteDocument(Long documentId);
}
