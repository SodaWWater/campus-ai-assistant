package com.liminghan.campusai.service;

import com.liminghan.campusai.entity.KbDocumentChunk;
import com.liminghan.campusai.vo.MatchedChunkVO;

import java.util.List;

public interface RagService {

    /** 返回原始 KbDocumentChunk 列表（兼容旧代码） */
    List<KbDocumentChunk> retrieveTopK(Long knowledgeBaseId, String question, int topK);

    /** 返回带来源信息和得分的 MatchedChunkVO 列表 */
    List<MatchedChunkVO> retrieveTopKWithScore(Long knowledgeBaseId, String question, int topK);
}
