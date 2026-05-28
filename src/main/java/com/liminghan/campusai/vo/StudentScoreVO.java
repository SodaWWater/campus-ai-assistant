package com.liminghan.campusai.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StudentScoreVO {

    private String studentNo;

    private String studentName;

    private String courseCode;

    private String courseName;

    private BigDecimal score;

    private String semester;
}
