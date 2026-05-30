package com.liminghan.campusai.service.impl;

import com.liminghan.campusai.entity.KbDocument;
import com.liminghan.campusai.entity.KbDocumentChunk;
import com.liminghan.campusai.service.KbDocumentChunkService;
import com.liminghan.campusai.service.KbDocumentService;
import com.liminghan.campusai.service.RagService;
import com.liminghan.campusai.util.KeywordMatcher;
import com.liminghan.campusai.vo.MatchedChunkVO;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RagServiceImpl implements RagService {

    private final KbDocumentChunkService chunkService;
    private final KbDocumentService documentService;
    private final KeywordMatcher keywordMatcher;

    public RagServiceImpl(KbDocumentChunkService chunkService,
                          KbDocumentService documentService,
                          KeywordMatcher keywordMatcher) {
        this.chunkService = chunkService;
        this.documentService = documentService;
        this.keywordMatcher = keywordMatcher;
    }

    @Override
    public List<KbDocumentChunk> retrieveTopK(Long knowledgeBaseId, String question, int topK) {
        List<KbDocumentChunk> chunks = chunkService.lambdaQuery()
                .eq(knowledgeBaseId != null, KbDocumentChunk::getKnowledgeBaseId, knowledgeBaseId)
                .list();
        return keywordMatcher.topK(question, chunks, topK);
    }

    @Override
    public List<MatchedChunkVO> retrieveTopKWithScore(Long knowledgeBaseId, String question, int topK) {
        List<KbDocumentChunk> chunks = chunkService.lambdaQuery()
                .eq(knowledgeBaseId != null, KbDocumentChunk::getKnowledgeBaseId, knowledgeBaseId)
                .list();

        if (chunks.isEmpty()) {
            return List.of();
        }

        // 批量加载文档标题
        Set<Long> docIds = new HashSet<>();
        for (KbDocumentChunk c : chunks) {
            docIds.add(c.getDocumentId());
        }
        Map<Long, String> titleMap = new HashMap<>();
        for (Long docId : docIds) {
            KbDocument doc = documentService.getById(docId);
            if (doc != null) {
                titleMap.put(docId, doc.getTitle());
            }
        }

        // 使用 KeywordMatcher 评分
        return keywordMatcher.topKWithScore(question, chunks, topK, titleMap);
    }
}
