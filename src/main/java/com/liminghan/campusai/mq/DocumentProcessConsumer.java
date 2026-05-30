package com.liminghan.campusai.mq;

import com.liminghan.campusai.dto.DocumentProcessMessage;
import com.liminghan.campusai.entity.KbDocument;
import com.liminghan.campusai.service.KbDocumentService;
import com.liminghan.campusai.service.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 文档处理消费者。
 * 主队列消息：正常处理（失败时 Spring Retry 自动重试 3 次后路由到 DLQ）。
 * DLQ 消息：重试已耗尽，标记文档 FAILED。
 */
@Component
public class DocumentProcessConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessConsumer.class);

    private final KnowledgeBaseService knowledgeBaseService;
    private final KbDocumentService documentService;

    public DocumentProcessConsumer(KnowledgeBaseService knowledgeBaseService,
                                   KbDocumentService documentService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentService = documentService;
    }

    /**
     * 主消费者：处理文档切分。异常向上抛出 → RetryTemplate 重试 → 耗尽后 Recoverer 投递 DLQ。
     */
    @RabbitListener(queues = "${app.mq.document-queue}")
    public void consume(DocumentProcessMessage message) {
        log.info("收到文档处理任务: documentId={}", message.getDocumentId());
        knowledgeBaseService.processDocumentChunks(message.getDocumentId());
        log.info("文档处理完成: documentId={}", message.getDocumentId());
    }

    /**
     * DLQ 消费者：收到死信意味着重试已耗尽，标记文档为 FAILED 并记录原因。
     */
    @RabbitListener(queues = "${app.mq.dlq-queue}")
    public void consumeDlq(DocumentProcessMessage message) {
        log.warn("文档处理进入死信队列，重试已耗尽: documentId={}", message.getDocumentId());
        try {
            KbDocument document = documentService.getById(message.getDocumentId());
            if (document != null && !"DONE".equals(document.getStatus())) {
                document.setStatus("FAILED");
                document.setErrorMessage("文档处理失败：已重试多次仍无法完成，请检查文件内容是否有效");
                document.setUpdatedAt(LocalDateTime.now());
                documentService.updateById(document);
            }
        } catch (Exception e) {
            log.error("更新死信文档状态失败: documentId={}", message.getDocumentId(), e);
        }
    }
}
