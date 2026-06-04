package com.liminghan.campusai.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Micrometer-based RAG pipeline metrics, exposed via Actuator /metrics endpoint.
 */
@Component
public class RagMetrics {

    private final Counter documentProcessed;
    private final Counter documentFailed;
    private final Counter llmSuccess;
    private final Counter llmFailure;
    private final Timer retrievalTimer;
    private final Timer generationTimer;

    public RagMetrics(MeterRegistry registry) {
        this.documentProcessed = Counter.builder("rag.documents.processed")
                .description("Number of documents successfully processed")
                .register(registry);

        this.documentFailed = Counter.builder("rag.documents.failed")
                .description("Number of documents that failed processing")
                .register(registry);

        this.llmSuccess = Counter.builder("rag.llm.success")
                .description("Number of successful LLM calls")
                .register(registry);

        this.llmFailure = Counter.builder("rag.llm.failure")
                .description("Number of failed LLM calls")
                .register(registry);

        this.retrievalTimer = Timer.builder("rag.retrieval.time")
                .description("Time taken for vector/keyword retrieval")
                .register(registry);

        this.generationTimer = Timer.builder("rag.generation.time")
                .description("Time taken for LLM generation")
                .register(registry);
    }

    public void recordDocumentProcessed() {
        documentProcessed.increment();
    }

    public void recordDocumentFailed() {
        documentFailed.increment();
    }

    public void recordLlmSuccess() {
        llmSuccess.increment();
    }

    public void recordLlmFailure() {
        llmFailure.increment();
    }

    public void recordRetrievalTime(long millis) {
        retrievalTimer.record(millis, TimeUnit.MILLISECONDS);
    }

    public void recordGenerationTime(long millis) {
        generationTimer.record(millis, TimeUnit.MILLISECONDS);
    }
}
