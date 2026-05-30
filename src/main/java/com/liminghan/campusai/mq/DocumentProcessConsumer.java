package com.liminghan.campusai.mq;

import com.liminghan.campusai.dto.DocumentProcessMessage;
import com.liminghan.campusai.service.KnowledgeBaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DocumentProcessConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessConsumer.class);

    private final KnowledgeBaseService knowledgeBaseService;

    public DocumentProcessConsumer(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @RabbitListener(queues = "${app.mq.document-queue}")
    public void consume(DocumentProcessMessage message) {
        log.info("收到文档处理任务: documentId={}", message.getDocumentId());
        knowledgeBaseService.processDocumentChunks(message.getDocumentId());
        log.info("文档处理完成: documentId={}", message.getDocumentId());
    }
}
