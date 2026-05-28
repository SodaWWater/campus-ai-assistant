package com.liminghan.campusai.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseAverageVO {

    private Long courseId;

    private String courseName;

    private BigDecimal averageScore;
}
