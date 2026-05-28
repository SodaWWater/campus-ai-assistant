package com.liminghan.campusai.controller;

import com.liminghan.campusai.common.Result;
import com.liminghan.campusai.service.AcademicService;
import com.liminghan.campusai.vo.CourseAverageVO;
import com.liminghan.campusai.vo.StudentScoreVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/academic")
public class AcademicController {

    private final AcademicService academicService;

    public AcademicController(AcademicService academicService) {
        this.academicService = academicService;
    }

    @Operation(summary = "List scores by student number")
    @GetMapping("/student/{studentNo}/scores")
    public Result<List<StudentScoreVO>> listStudentScores(@PathVariable String studentNo) {
        return Result.success(academicService.listStudentScores(studentNo));
    }

    @Operation(summary = "Get average score by course id")
    @GetMapping("/course/{courseId}/average")
    public Result<CourseAverageVO> getCourseAverage(@PathVariable Long courseId) {
        return Result.success(academicService.getCourseAverage(courseId));
    }
}
