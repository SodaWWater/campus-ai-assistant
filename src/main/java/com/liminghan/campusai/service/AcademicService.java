package com.liminghan.campusai.service;

import com.liminghan.campusai.vo.CourseAverageVO;
import com.liminghan.campusai.vo.StudentScoreVO;

import java.util.List;

public interface AcademicService {

    List<StudentScoreVO> listStudentScores(String studentNo);

    CourseAverageVO getCourseAverage(Long courseId);

    String answerAcademicQuestion(String question);
}
