package com.liminghan.campusai.service;

import com.liminghan.campusai.entity.KbDocumentChunk;

import java.util.List;

public interface RagService {

    List<KbDocumentChunk> retrieveTopK(Long knowledgeBaseId, String question, int topK);
}
