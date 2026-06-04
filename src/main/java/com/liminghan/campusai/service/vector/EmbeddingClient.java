package com.liminghan.campusai.service.vector;

public interface EmbeddingClient {

    EmbeddingVector embed(String text);
}
