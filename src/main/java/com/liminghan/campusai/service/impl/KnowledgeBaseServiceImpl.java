package com.liminghan.campusai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liminghan.campusai.common.BusinessException;
import com.liminghan.campusai.common.ErrorCode;
import com.liminghan.campusai.dto.DocumentCreateRequest;
import com.liminghan.campusai.dto.KnowledgeBaseCreateRequest;
import com.liminghan.campusai.entity.KbDocument;
import com.liminghan.campusai.entity.KbDocumentChunk;
import com.liminghan.campusai.entity.KnowledgeBase;
import com.liminghan.campusai.mapper.KnowledgeBaseMapper;
import com.liminghan.campusai.service.KbDocumentChunkService;
import com.liminghan.campusai.service.KbDocumentService;
import com.liminghan.campusai.service.KnowledgeBaseService;
import com.liminghan.campusai.util.KeywordMatcher;
import com.liminghan.campusai.util.TextChunker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {

    private final KbDocumentService documentService;
    private final KbDocumentChunkService chunkService;
    private final TextChunker textChunker;
    private final KeywordMatcher keywordMatcher;

    public KnowledgeBaseServiceImpl(KbDocumentService documentService,
                                    KbDocumentChunkService chunkService,
                                    TextChunker textChunker,
                                    KeywordMatcher keywordMatcher) {
        this.documentService = documentService;
        this.chunkService = chunkService;
        this.textChunker = textChunker;
        this.keywordMatcher = keywordMatcher;
    }

    @Override
    public KnowledgeBase createKnowledgeBase(KnowledgeBaseCreateRequest request) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName(request.getName());
        knowledgeBase.setDescription(request.getDescription());
        knowledgeBase.setCreatedAt(LocalDateTime.now());
        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        save(knowledgeBase);
        return knowledgeBase;
    }

    @Override
    public List<KnowledgeBase> listKnowledgeBases() {
        return lambdaQuery().orderByDesc(KnowledgeBase::getCreatedAt).list();
    }

    @Override
    public KnowledgeBase getKnowledgeBase(Long id) {
        KnowledgeBase knowledgeBase = getById(id);
        if (knowledgeBase == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "knowledge base not found");
        }
        return knowledgeBase;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(Long id) {
        getKnowledgeBase(id);
        documentService.lambdaUpdate().eq(KbDocument::getKnowledgeBaseId, id).remove();
        chunkService.lambdaUpdate().eq(KbDocumentChunk::getKnowledgeBaseId, id).remove();
        removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addDocument(Long knowledgeBaseId, DocumentCreateRequest request) {
        getKnowledgeBase(knowledgeBaseId);
        KbDocument document = new KbDocument();
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setTitle(request.getTitle());
        document.setContent(request.getContent());
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        documentService.save(document);

        List<String> chunks = textChunker.split(request.getContent());
        for (int i = 0; i < chunks.size(); i++) {
            KbDocumentChunk chunk = new KbDocumentChunk();
            chunk.setDocumentId(document.getId());
            chunk.setKnowledgeBaseId(knowledgeBaseId);
            chunk.setChunkIndex(i);
            chunk.setContent(chunks.get(i));
            chunk.setKeywords(keywordMatcher.extractKeywords(chunks.get(i)));
            chunk.setCreatedAt(LocalDateTime.now());
            chunkService.save(chunk);
        }
        return document.getId();
    }

    @Override
    public List<KbDocumentChunk> listChunks(Long knowledgeBaseId) {
        getKnowledgeBase(knowledgeBaseId);
        return chunkService.lambdaQuery()
                .eq(KbDocumentChunk::getKnowledgeBaseId, knowledgeBaseId)
                .orderByAsc(KbDocumentChunk::getChunkIndex)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId) {
        KbDocument document = documentService.getById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "document not found");
        }
        chunkService.lambdaUpdate().eq(KbDocumentChunk::getDocumentId, documentId).remove();
        documentService.removeById(documentId);
    }
}
