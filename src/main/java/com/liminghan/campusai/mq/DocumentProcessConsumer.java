package com.liminghan.campusai.mq;

import com.liminghan.campusai.dto.DocumentProcessMessage;
import com.liminghan.campusai.service.KnowledgeBaseService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DocumentProcessConsumer {

    private final KnowledgeBaseService knowledgeBaseService;

    public DocumentProcessConsumer(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @RabbitListener(queues = "${app.mq.document-queue}")
    public void consume(DocumentProcessMessage message) {
        knowledgeBaseService.processDocumentChunks(message.getDocumentId());
    }
}
