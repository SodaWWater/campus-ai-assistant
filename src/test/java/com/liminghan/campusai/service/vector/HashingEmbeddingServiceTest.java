package com.liminghan.campusai.service.vector;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashingEmbeddingServiceTest {

    @Test
    void shouldReturnConfiguredDimension() {
        HashingEmbeddingService service = new HashingEmbeddingService(128);

        EmbeddingVector embedding = service.embed("ArrayList 和 LinkedList 的区别");

        assertThat(embedding.values()).hasSize(128);
        assertThat(embedding.dimension()).isEqualTo(128);
        assertThat(embedding.provider()).isEqualTo("local");
        assertThat(embedding.model()).isEqualTo("hashing-embedding");
        assertThat(service.dimension()).isEqualTo(128);
        assertThat(service.provider()).isEqualTo("local");
        assertThat(service.model()).isEqualTo("hashing-embedding");
    }

    @Test
    void shouldNormalizeNonEmptyEmbedding() {
        HashingEmbeddingService service = new HashingEmbeddingService(64);

        EmbeddingVector embedding = service.embed("Java database transaction ACID");

        double norm = 0;
        for (double value : embedding.values()) {
            norm += value * value;
        }
        assertThat(Math.sqrt(norm)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.000001));
    }
}
