package com.liminghan.campusai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.liminghan.campusai.common.BusinessException;
import com.liminghan.campusai.common.ErrorCode;
import com.liminghan.campusai.dto.DocumentProcessMessage;
import com.liminghan.campusai.dto.DocumentCreateRequest;
import com.liminghan.campusai.dto.KnowledgeBaseCreateRequest;
import com.liminghan.campusai.entity.KbDocument;
import com.liminghan.campusai.entity.KbDocumentChunk;
import com.liminghan.campusai.entity.KnowledgeBase;
import com.liminghan.campusai.mapper.KnowledgeBaseMapper;
import com.liminghan.campusai.service.KbDocumentChunkService;
import com.liminghan.campusai.service.KbDocumentService;
import com.liminghan.campusai.service.KnowledgeBaseService;
import com.liminghan.campusai.service.vector.PgVectorSearchService;
import com.liminghan.campusai.util.KeywordMatcher;
import com.liminghan.campusai.util.TextChunker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {

    private final KbDocumentService documentService;
    private final KbDocumentChunkService chunkService;
    private final TextChunker textChunker;
    private final KeywordMatcher keywordMatcher;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final PgVectorSearchService vectorSearchService;

    @Value("${app.cache.knowledge-base-list-ttl-minutes:10}")
    private long knowledgeBaseListTtlMinutes;

    @Value("${app.mq.document-exchange}")
    private String documentExchange;

    @Value("${app.mq.document-routing-key}")
    private String documentRoutingKey;

    public KnowledgeBaseServiceImpl(KbDocumentService documentService,
                                    KbDocumentChunkService chunkService,
                                    TextChunker textChunker,
                                    KeywordMatcher keywordMatcher,
                                    RedisTemplate<String, Object> redisTemplate,
                                    RabbitTemplate rabbitTemplate,
                                    PgVectorSearchService vectorSearchService) {
        this.documentService = documentService;
        this.chunkService = chunkService;
        this.textChunker = textChunker;
        this.keywordMatcher = keywordMatcher;
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
        this.vectorSearchService = vectorSearchService;
    }

    @Override
    public KnowledgeBase createKnowledgeBase(KnowledgeBaseCreateRequest request) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setVisibility(request.getVisibility() != null ? request.getVisibility() : "PUBLIC");
        kb.setOwnerId(0L); // TODO: Phase 5 — 从 SecurityContext 获取当前用户
        kb.setOwnerName("system");
        kb.setDocumentCount(0);
        kb.setChunkCount(0);
        kb.setCreatedAt(LocalDateTime.now());
        kb.setUpdatedAt(LocalDateTime.now());
        save(kb);
        evictKnowledgeBaseCache();
        return kb;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<KnowledgeBase> listKnowledgeBases() {
        String cacheKey = "kb:list";
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List<?> list) {
                return (List<KnowledgeBase>) list;
            }
        } catch (Exception ignored) {
            // Redis is an optimization. Fall back to MySQL when unavailable.
        }
        List<KnowledgeBase> list = lambdaQuery().orderByDesc(KnowledgeBase::getCreatedAt).list();
        try {
            redisTemplate.opsForValue().set(cacheKey, list, Duration.ofMinutes(knowledgeBaseListTtlMinutes));
        } catch (Exception ignored) {
        }
        return list;
    }

    @Override
    public KnowledgeBase getKnowledgeBase(Long id) {
        KnowledgeBase kb = getById(id);
        if (kb == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "knowledge base not found");
        }
        return kb;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(Long id) {
        getKnowledgeBase(id);
        vectorSearchService.deleteByKnowledgeBaseId(id);
        documentService.lambdaUpdate().eq(KbDocument::getKnowledgeBaseId, id).remove();
        chunkService.lambdaUpdate().eq(KbDocumentChunk::getKnowledgeBaseId, id).remove();
        removeById(id);
        evictKnowledgeBaseCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addDocument(Long knowledgeBaseId, DocumentCreateRequest request) {
        Long documentId = createDocumentRecord(knowledgeBaseId, request.getTitle(),
                request.getContent(), "manual.txt", "txt", (long) request.getContent().length());
        processDocumentChunks(documentId);
        return documentId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long uploadDocumentAsync(Long knowledgeBaseId, String fileName, String fileType, Long fileSize, String content) {
        String title = fileName.lastIndexOf('.') > 0 ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
        Long documentId = createDocumentRecord(knowledgeBaseId, title, content, fileName, fileType, fileSize);

        // 纯异步：投递 MQ，由 Consumer 异步处理，上传接口立即返回
        DocumentProcessMessage message = new DocumentProcessMessage();
        message.setDocumentId(documentId);
        message.setKnowledgeBaseId(knowledgeBaseId);
        try {
            rabbitTemplate.convertAndSend(documentExchange, documentRoutingKey, message);
        } catch (Exception e) {
            // MQ 不可用时降级为同步处理
            try {
                processDocumentChunks(documentId);
            } catch (Exception pe) {
                markDocumentFailed(documentId, pe.getMessage());
            }
        }
        return documentId;
    }

    private Long createDocumentRecord(Long knowledgeBaseId, String title, String content,
                                       String fileName, String fileType, Long fileSize) {
        getKnowledgeBase(knowledgeBaseId);
        KbDocument document = new KbDocument();
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setTitle(title);
        document.setContent(content);
        document.setFileName(fileName);
        document.setFileType(fileType);
        document.setFileSize(fileSize);
        document.setStatus("PROCESSING");
        document.setChunkCount(0);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        documentService.save(document);
        // 立即更新知识库文档计数
        updateKbCounts(knowledgeBaseId);
        return document.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processDocumentChunks(Long documentId) {
        KbDocument document = documentService.getById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "document not found");
        }

        vectorSearchService.deleteByDocumentId(documentId);
        chunkService.lambdaUpdate().eq(KbDocumentChunk::getDocumentId, documentId).remove();
        List<String> chunks = textChunker.split(document.getContent());
        List<KbDocumentChunk> savedChunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            KbDocumentChunk chunk = new KbDocumentChunk();
            chunk.setDocumentId(document.getId());
            chunk.setKnowledgeBaseId(document.getKnowledgeBaseId());
            chunk.setChunkIndex(i);
            chunk.setContent(chunks.get(i));
            chunk.setKeywords(keywordMatcher.extractKeywords(chunks.get(i)));
            chunk.setCreatedAt(LocalDateTime.now());
            chunkService.save(chunk);
            savedChunks.add(chunk);
        }

        // 更新文档状态为 DONE
        document.setStatus("DONE");
        document.setChunkCount(chunks.size());
        document.setProcessedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        documentService.updateById(document);

        // 更新知识库计数
        updateKbCounts(document.getKnowledgeBaseId());
        vectorSearchService.indexChunks(savedChunks, Map.of(document.getId(), document.getTitle()));
    }

    /**
     * 标记文档处理失败（在事务外调用，确保 FAILED 状态持久化）
     */
    private void markDocumentFailed(Long documentId, String errorMessage) {
        try {
            KbDocument document = documentService.getById(documentId);
            if (document != null && !"DONE".equals(document.getStatus())) {
                document.setStatus("FAILED");
                document.setErrorMessage(errorMessage);
                document.setUpdatedAt(LocalDateTime.now());
                documentService.updateById(document);
            }
        } catch (Exception ignored) {
        }
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
    public List<KbDocument> listDocuments(Long knowledgeBaseId) {
        getKnowledgeBase(knowledgeBaseId);
        return documentService.lambdaQuery()
                .eq(KbDocument::getKnowledgeBaseId, knowledgeBaseId)
                .orderByDesc(KbDocument::getCreatedAt)
                .list();
    }

    @Override
    public List<KbDocumentChunk> listDocumentChunks(Long documentId) {
        KbDocument document = documentService.getById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "document not found");
        }
        return chunkService.lambdaQuery()
                .eq(KbDocumentChunk::getDocumentId, documentId)
                .orderByAsc(KbDocumentChunk::getChunkIndex)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reprocessDocument(Long documentId) {
        KbDocument document = documentService.getById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "document not found");
        }
        document.setStatus("PROCESSING");
        document.setErrorMessage(null);
        document.setUpdatedAt(LocalDateTime.now());
        documentService.updateById(document);

        // 纯异步：重置状态后投递 MQ 重新处理
        DocumentProcessMessage message = new DocumentProcessMessage();
        message.setDocumentId(documentId);
        message.setKnowledgeBaseId(document.getKnowledgeBaseId());
        try {
            rabbitTemplate.convertAndSend(documentExchange, documentRoutingKey, message);
        } catch (Exception e) {
            // MQ 不可用时降级为同步处理
            try {
                processDocumentChunks(documentId);
            } catch (Exception pe) {
                markDocumentFailed(documentId, pe.getMessage());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId) {
        KbDocument document = documentService.getById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "document not found");
        }
        vectorSearchService.deleteByDocumentId(documentId);
        chunkService.lambdaUpdate().eq(KbDocumentChunk::getDocumentId, documentId).remove();
        documentService.removeById(documentId);
        updateKbCounts(document.getKnowledgeBaseId());
    }

    private void updateKbCounts(Long knowledgeBaseId) {
        long docCount = documentService.lambdaQuery()
                .eq(KbDocument::getKnowledgeBaseId, knowledgeBaseId).count();
        long chunkCount = chunkService.lambdaQuery()
                .eq(KbDocumentChunk::getKnowledgeBaseId, knowledgeBaseId).count();
        KnowledgeBase kb = getById(knowledgeBaseId);
        if (kb != null) {
            kb.setDocumentCount((int) docCount);
            kb.setChunkCount((int) chunkCount);
            updateById(kb);
        }
        evictKnowledgeBaseCache();
    }

    private void evictKnowledgeBaseCache() {
        try {
            redisTemplate.delete("kb:list");
        } catch (Exception ignored) {
        }
    }
}
