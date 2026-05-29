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
import com.liminghan.campusai.util.KeywordMatcher;
import com.liminghan.campusai.util.TextChunker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {

    private final KbDocumentService documentService;
    private final KbDocumentChunkService chunkService;
    private final TextChunker textChunker;
    private final KeywordMatcher keywordMatcher;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;

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
                                    RabbitTemplate rabbitTemplate) {
        this.documentService = documentService;
        this.chunkService = chunkService;
        this.textChunker = textChunker;
        this.keywordMatcher = keywordMatcher;
        this.redisTemplate = redisTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public KnowledgeBase createKnowledgeBase(KnowledgeBaseCreateRequest request) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setName(request.getName());
        knowledgeBase.setDescription(request.getDescription());
        knowledgeBase.setCreatedAt(LocalDateTime.now());
        knowledgeBase.setUpdatedAt(LocalDateTime.now());
        save(knowledgeBase);
        evictKnowledgeBaseCache();
        return knowledgeBase;
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
            // Cache failure should not break knowledge base listing.
        }
        return list;
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
        evictKnowledgeBaseCache();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addDocument(Long knowledgeBaseId, DocumentCreateRequest request) {
        Long documentId = createDocumentRecord(knowledgeBaseId, request);
        processDocumentChunks(documentId);
        return documentId;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long uploadDocumentAsync(Long knowledgeBaseId, DocumentCreateRequest request) {
        Long documentId = createDocumentRecord(knowledgeBaseId, request);
        DocumentProcessMessage message = new DocumentProcessMessage();
        message.setDocumentId(documentId);
        message.setKnowledgeBaseId(knowledgeBaseId);
        try {
            rabbitTemplate.convertAndSend(documentExchange, documentRoutingKey, message);
        } catch (Exception ignored) {
            processDocumentChunks(documentId);
        }
        return documentId;
    }

    private Long createDocumentRecord(Long knowledgeBaseId, DocumentCreateRequest request) {
        getKnowledgeBase(knowledgeBaseId);
        KbDocument document = new KbDocument();
        document.setKnowledgeBaseId(knowledgeBaseId);
        document.setTitle(request.getTitle());
        document.setContent(request.getContent());
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        documentService.save(document);
        return document.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processDocumentChunks(Long documentId) {
        KbDocument document = documentService.getById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "document not found");
        }
        chunkService.lambdaUpdate().eq(KbDocumentChunk::getDocumentId, documentId).remove();
        List<String> chunks = textChunker.split(document.getContent());
        for (int i = 0; i < chunks.size(); i++) {
            KbDocumentChunk chunk = new KbDocumentChunk();
            chunk.setDocumentId(document.getId());
            chunk.setKnowledgeBaseId(document.getKnowledgeBaseId());
            chunk.setChunkIndex(i);
            chunk.setContent(chunks.get(i));
            chunk.setKeywords(keywordMatcher.extractKeywords(chunks.get(i)));
            chunk.setCreatedAt(LocalDateTime.now());
            chunkService.save(chunk);
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
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId) {
        KbDocument document = documentService.getById(documentId);
        if (document == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "document not found");
        }
        chunkService.lambdaUpdate().eq(KbDocumentChunk::getDocumentId, documentId).remove();
        documentService.removeById(documentId);
    }

    private void evictKnowledgeBaseCache() {
        try {
            redisTemplate.delete("kb:list");
        } catch (Exception ignored) {
            // Redis unavailable: no cache to evict.
        }
    }
}
