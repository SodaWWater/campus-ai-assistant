package com.liminghan.campusai.service.impl;

import com.liminghan.campusai.entity.KbDocument;
import com.liminghan.campusai.entity.KbDocumentChunk;
import com.liminghan.campusai.service.KbDocumentChunkService;
import com.liminghan.campusai.service.KbDocumentService;
import com.liminghan.campusai.service.RagService;
import com.liminghan.campusai.service.vector.PgVectorSearchService;
import com.liminghan.campusai.util.KeywordMatcher;
import com.liminghan.campusai.vo.MatchedChunkVO;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RagServiceImpl implements RagService {

    private final KbDocumentChunkService chunkService;
    private final KbDocumentService documentService;
    private final KeywordMatcher keywordMatcher;
    private final PgVectorSearchService vectorSearchService;

    public RagServiceImpl(KbDocumentChunkService chunkService,
                          KbDocumentService documentService,
                          KeywordMatcher keywordMatcher,
                          PgVectorSearchService vectorSearchService) {
        this.chunkService = chunkService;
        this.documentService = documentService;
        this.keywordMatcher = keywordMatcher;
        this.vectorSearchService = vectorSearchService;
    }

    @Override
    public List<KbDocumentChunk> retrieveTopK(Long knowledgeBaseId, String question, int topK) {
        List<KbDocumentChunk> chunks = loadChunks(knowledgeBaseId);
        if (chunks.isEmpty()) {
            return List.of();
        }

        Map<Long, String> titleMap = loadTitleMap(chunks);
        if (knowledgeBaseId != null) {
            vectorSearchService.indexChunks(chunks, titleMap);
            List<MatchedChunkVO> vectorResults = vectorSearchService.search(knowledgeBaseId, question, topK);
            if (!vectorResults.isEmpty()) {
                Map<Long, KbDocumentChunk> chunkMap = chunks.stream()
                        .collect(Collectors.toMap(KbDocumentChunk::getId, Function.identity(), (a, b) -> a));
                return vectorResults.stream()
                        .map(result -> chunkMap.get(result.getId()))
                        .filter(Objects::nonNull)
                        .toList();
            }
        }

        return keywordMatcher.topK(question, chunks, topK);
    }

    @Override
    public List<MatchedChunkVO> retrieveTopKWithScore(Long knowledgeBaseId, String question, int topK) {
        List<KbDocumentChunk> chunks = loadChunks(knowledgeBaseId);
        if (chunks.isEmpty()) {
            return List.of();
        }

        Map<Long, String> titleMap = loadTitleMap(chunks);
        if (knowledgeBaseId != null) {
            vectorSearchService.indexChunks(chunks, titleMap);
            List<MatchedChunkVO> vectorResults = vectorSearchService.search(knowledgeBaseId, question, topK);
            if (!vectorResults.isEmpty()) {
                return vectorResults;
            }
        }

        return keywordMatcher.topKWithScore(question, chunks, topK, titleMap);
    }

    private List<KbDocumentChunk> loadChunks(Long knowledgeBaseId) {
        return chunkService.lambdaQuery()
                .eq(knowledgeBaseId != null, KbDocumentChunk::getKnowledgeBaseId, knowledgeBaseId)
                .list();
    }

    private Map<Long, String> loadTitleMap(List<KbDocumentChunk> chunks) {
        Set<Long> docIds = chunks.stream()
                .map(KbDocumentChunk::getDocumentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (docIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> titleMap = new HashMap<>();
        for (Long docId : docIds) {
            KbDocument doc = documentService.getById(docId);
            if (doc != null) {
                titleMap.put(docId, doc.getTitle());
            }
        }
        return titleMap;
    }
}
