package com.liminghan.campusai.service.impl;

import com.liminghan.campusai.entity.KbDocumentChunk;
import com.liminghan.campusai.service.KbDocumentChunkService;
import com.liminghan.campusai.service.RagService;
import com.liminghan.campusai.util.KeywordMatcher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RagServiceImpl implements RagService {

    private final KbDocumentChunkService chunkService;
    private final KeywordMatcher keywordMatcher;

    public RagServiceImpl(KbDocumentChunkService chunkService, KeywordMatcher keywordMatcher) {
        this.chunkService = chunkService;
        this.keywordMatcher = keywordMatcher;
    }

    @Override
    public List<KbDocumentChunk> retrieveTopK(Long knowledgeBaseId, String question, int topK) {
        List<KbDocumentChunk> chunks = chunkService.lambdaQuery()
                .eq(knowledgeBaseId != null, KbDocumentChunk::getKnowledgeBaseId, knowledgeBaseId)
                .list();
        return keywordMatcher.topK(question, chunks, topK);
    }
}
