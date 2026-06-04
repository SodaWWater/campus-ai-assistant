package com.liminghan.campusai.service.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OpenAiCompatibleEmbeddingClient implements EmbeddingClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String provider;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final int dimension;
    private final boolean sendDimensions;

    public OpenAiCompatibleEmbeddingClient(ObjectMapper objectMapper,
                                           @Value("${app.embedding.provider:openai-compatible}") String provider,
                                           @Value("${app.embedding.base-url:}") String baseUrl,
                                           @Value("${app.embedding.api-key:}") String apiKey,
                                           @Value("${app.embedding.model:text-embedding-3-small}") String model,
                                           @Value("${app.embedding.dimension:128}") int dimension,
                                           @Value("${app.embedding.send-dimensions:true}") boolean sendDimensions) {
        this.objectMapper = objectMapper;
        this.provider = provider;
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.apiKey = apiKey;
        this.model = model;
        this.dimension = dimension;
        this.sendDimensions = sendDimensions;
        this.restClient = RestClient.builder().build();
    }

    @Override
    public EmbeddingVector embed(String text) {
        if (!isConfigured()) {
            throw new IllegalStateException("Embedding base-url and api-key must be configured for real embedding.");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("input", text == null ? "" : text);
        if (sendDimensions) {
            body.put("dimensions", dimension);
        }

        String response = restClient.post()
                .uri(baseUrl + "/embeddings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        double[] embedding = parseEmbedding(response);
        return new EmbeddingVector(embedding, provider, model, dimension);
    }

    public int dimension() {
        return dimension;
    }

    public String provider() {
        return provider;
    }

    public String model() {
        return model;
    }

    public boolean isConfigured() {
        return baseUrl != null && !baseUrl.isBlank()
                && apiKey != null && !apiKey.isBlank();
    }

    private double[] parseEmbedding(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode embeddingNode = root.path("data").path(0).path("embedding");
            if (!embeddingNode.isArray()) {
                throw new IllegalStateException("Embedding response does not contain data[0].embedding.");
            }
            if (embeddingNode.size() != dimension) {
                throw new IllegalStateException("Embedding dimension mismatch. expected="
                        + dimension + ", actual=" + embeddingNode.size());
            }
            double[] embedding = new double[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                embedding[i] = embeddingNode.get(i).asDouble();
            }
            return embedding;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse embedding response: " + e.getMessage(), e);
        }
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
