package com.liminghan.campusai.service.vector;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingClientRouterTest {

    @Test
    void autoModeShouldFallbackToHashingWhenRealEmbeddingIsNotConfigured() {
        HashingEmbeddingService hashing = new HashingEmbeddingService(128);
        OpenAiCompatibleEmbeddingClient real = new OpenAiCompatibleEmbeddingClient(
                new ObjectMapper(),
                "openai-compatible",
                "",
                "",
                "text-embedding-3-small",
                128,
                true
        );
        EmbeddingClientRouter router = new EmbeddingClientRouter("auto", hashing, real);

        EmbeddingVector embedding = router.embed("RAG 文档切片和向量检索");

        assertThat(embedding.values()).hasSize(128);
        assertThat(embedding.dimension()).isEqualTo(128);
        assertThat(embedding.provider()).isEqualTo("local");
        assertThat(embedding.model()).isEqualTo("hashing-embedding");
        assertThat(router.provider()).isEqualTo("local");
        assertThat(router.model()).isEqualTo("hashing-embedding");
    }
}
