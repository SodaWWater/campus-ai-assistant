package com.liminghan.campusai.vo;

import lombok.Data;

@Data
public class MatchedChunkVO {

    private Long id;

    private Integer chunkIndex;

    private String content;
}
