package com.liminghan.campusai.service.vector;

public record EmbeddingVector(double[] values, String provider, String model, int dimension) {

    public EmbeddingVector {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("embedding values must not be empty");
        }
        if (dimension <= 0) {
            throw new IllegalArgumentException("embedding dimension must be positive");
        }
        if (values.length != dimension) {
            throw new IllegalArgumentException("embedding values length must equal dimension");
        }
    }
}
