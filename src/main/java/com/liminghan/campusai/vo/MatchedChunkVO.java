package com.liminghan.campusai.vo;

import lombok.Data;

@Data
public class MatchedChunkVO {

    private Long id;

    private Integer chunkIndex;

    private String content;

    /** 来源文档标题 */
    private String documentTitle;

    /** 关键词命中得分 */
    private int score;
}
