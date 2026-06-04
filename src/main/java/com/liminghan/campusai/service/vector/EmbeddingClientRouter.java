package com.liminghan.campusai.service.vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingClientRouter implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClientRouter.class);

    private final String mode;
    private final HashingEmbeddingService hashingEmbeddingService;
    private final OpenAiCompatibleEmbeddingClient openAiCompatibleEmbeddingClient;

    public EmbeddingClientRouter(@Value("${app.embedding.mode:hashing}") String mode,
                                 HashingEmbeddingService hashingEmbeddingService,
                                 OpenAiCompatibleEmbeddingClient openAiCompatibleEmbeddingClient) {
        this.mode = mode == null ? "hashing" : mode.trim().toLowerCase();
        this.hashingEmbeddingService = hashingEmbeddingService;
        this.openAiCompatibleEmbeddingClient = openAiCompatibleEmbeddingClient;
    }

    @Override
    public EmbeddingVector embed(String text) {
        return switch (mode) {
            case "openai-compatible", "real" -> openAiCompatibleEmbeddingClient.embed(text);
            case "auto" -> embedWithAutoFallback(text);
            case "hashing", "mock" -> hashingEmbeddingService.embed(text);
            default -> {
                log.warn("Unknown embedding mode '{}', falling back to hashing embedding.", mode);
                yield hashingEmbeddingService.embed(text);
            }
        };
    }

    public int dimension() {
        return switch (mode) {
            case "openai-compatible", "real" -> openAiCompatibleEmbeddingClient.dimension();
            case "auto" -> openAiCompatibleEmbeddingClient.isConfigured()
                    ? openAiCompatibleEmbeddingClient.dimension()
                    : hashingEmbeddingService.dimension();
            default -> hashingEmbeddingService.dimension();
        };
    }

    public String provider() {
        return switch (mode) {
            case "openai-compatible", "real" -> openAiCompatibleEmbeddingClient.provider();
            case "auto" -> openAiCompatibleEmbeddingClient.isConfigured()
                    ? openAiCompatibleEmbeddingClient.provider()
                    : hashingEmbeddingService.provider();
            default -> hashingEmbeddingService.provider();
        };
    }

    public String model() {
        return switch (mode) {
            case "openai-compatible", "real" -> openAiCompatibleEmbeddingClient.model();
            case "auto" -> openAiCompatibleEmbeddingClient.isConfigured()
                    ? openAiCompatibleEmbeddingClient.model()
                    : hashingEmbeddingService.model();
            default -> hashingEmbeddingService.model();
        };
    }

    private EmbeddingVector embedWithAutoFallback(String text) {
        if (!openAiCompatibleEmbeddingClient.isConfigured()) {
            return hashingEmbeddingService.embed(text);
        }
        try {
            return openAiCompatibleEmbeddingClient.embed(text);
        } catch (Exception e) {
            log.warn("Real embedding failed in auto mode, falling back to hashing embedding. Cause: {}", e.getMessage());
            return hashingEmbeddingService.embed(text);
        }
    }
}
