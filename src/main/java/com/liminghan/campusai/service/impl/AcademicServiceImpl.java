package com.liminghan.campusai.service.impl;

import com.liminghan.campusai.common.BusinessException;
import com.liminghan.campusai.common.ErrorCode;
import com.liminghan.campusai.entity.Course;
import com.liminghan.campusai.entity.Score;
import com.liminghan.campusai.entity.Student;
import com.liminghan.campusai.service.AcademicService;
import com.liminghan.campusai.service.CourseService;
import com.liminghan.campusai.service.ScoreService;
import com.liminghan.campusai.service.StudentService;
import com.liminghan.campusai.vo.CourseAverageVO;
import com.liminghan.campusai.vo.StudentScoreVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AcademicServiceImpl implements AcademicService {

    private final StudentService studentService;
    private final CourseService courseService;
    private final ScoreService scoreService;

    public AcademicServiceImpl(StudentService studentService, CourseService courseService, ScoreService scoreService) {
        this.studentService = studentService;
        this.courseService = courseService;
        this.scoreService = scoreService;
    }

    @Override
    public List<StudentScoreVO> listStudentScores(String studentNo) {
        Student student = studentService.lambdaQuery().eq(Student::getStudentNo, studentNo).one();
        if (student == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "student not found");
        }
        List<Score> scores = scoreService.lambdaQuery().eq(Score::getStudentId, student.getId()).list();
        List<StudentScoreVO> result = new ArrayList<>();
        for (Score score : scores) {
            Course course = courseService.getById(score.getCourseId());
            StudentScoreVO vo = new StudentScoreVO();
            vo.setStudentNo(student.getStudentNo());
            vo.setStudentName(student.getName());
            vo.setCourseCode(course == null ? null : course.getCourseCode());
            vo.setCourseName(course == null ? null : course.getCourseName());
            vo.setScore(score.getScore());
            vo.setSemester(score.getSemester());
            result.add(vo);
        }
        return result;
    }

    @Override
    public CourseAverageVO getCourseAverage(Long courseId) {
        Course course = courseService.getById(courseId);
        if (course == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "course not found");
        }
        List<Score> scores = scoreService.lambdaQuery().eq(Score::getCourseId, courseId).list();
        BigDecimal average = BigDecimal.ZERO;
        if (!scores.isEmpty()) {
            BigDecimal total = scores.stream()
                    .map(Score::getScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            average = total.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
        }
        CourseAverageVO vo = new CourseAverageVO();
        vo.setCourseId(course.getId());
        vo.setCourseName(course.getCourseName());
        vo.setAverageScore(average);
        return vo;
    }

    @Override
    public String answerAcademicQuestion(String question) {
        Optional<Student> matchedStudent = studentService.list().stream()
                .filter(student -> question.contains(student.getStudentNo()) || question.contains(student.getName()))
                .findFirst();
        if (matchedStudent.isPresent()) {
            List<StudentScoreVO> scores = listStudentScores(matchedStudent.get().getStudentNo());
            return "根据数据库查询，" + matchedStudent.get().getName() + "的成绩记录为：" + scores;
        }

        Optional<Course> matchedCourse = courseService.list().stream()
                .filter(course -> question.contains(String.valueOf(course.getId()))
                        || question.contains(course.getCourseCode())
                        || question.contains(course.getCourseName()))
                .findFirst();
        if (matchedCourse.isPresent()) {
            CourseAverageVO average = getCourseAverage(matchedCourse.get().getId());
            return "根据数据库查询，" + average.getCourseName() + "的平均分为：" + average.getAverageScore();
        }

        return "这是学业数据类问题，但当前没有匹配到明确的学生或课程，请提供学生姓名、学号或课程名称。";
    }
}
