package com.liminghan.campusai.vo;

import lombok.Data;

import java.util.List;

@Data
public class ChatResponseVO {

    private String answer;

    private String sourceType;

    private Long conversationId;

    private String promptPreview;

    private List<MatchedChunkVO> matchedChunks;
}
