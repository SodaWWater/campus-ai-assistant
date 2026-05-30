package com.liminghan.campusai.controller;

import com.liminghan.campusai.common.Result;
import com.liminghan.campusai.entity.*;
import com.liminghan.campusai.service.*;
import com.liminghan.campusai.vo.CourseAverageVO;
import com.liminghan.campusai.vo.StudentScoreVO;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@Tag(name = "学生端", description = "学生首页、知识库浏览、学业查询")
@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final ChatRecordService chatRecordService;
    private final StudentService studentService;
    private final ScoreService scoreService;
    private final CourseService courseService;

    public StudentController(KnowledgeBaseService knowledgeBaseService,
                             ChatRecordService chatRecordService,
                             StudentService studentService,
                             ScoreService scoreService,
                             CourseService courseService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.chatRecordService = chatRecordService;
        this.studentService = studentService;
        this.scoreService = scoreService;
        this.courseService = courseService;
    }

    @Operation(summary = "学生首页数据")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        List<KnowledgeBase> allKbs = knowledgeBaseService.listKnowledgeBases();
        long chatCount = chatRecordService.count();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("kbCount", allKbs.size());
        data.put("chatCount", chatCount);
        int docCount = allKbs.stream().mapToInt(kb -> kb.getDocumentCount() != null ? kb.getDocumentCount() : 0).sum();
        data.put("docCount", docCount);
        data.put("recentKnowledgeBases", allKbs.stream().limit(5).toList());
        return Result.success(data);
    }

    @Operation(summary = "学生可见知识库")
    @GetMapping("/knowledge-bases")
    public Result<List<KnowledgeBase>> knowledgeBases() {
        return Result.success(knowledgeBaseService.listKnowledgeBases());
    }

    @Operation(summary = "当前登录学生的学业成绩（自动识别学号）")
    @GetMapping("/academic")
    public Result<Map<String, Object>> academic() {
        Long userId = getCurrentUserId();

        // 通过 user_id 关联查找当前登录学生
        Student student = studentService.lambdaQuery()
                .eq(Student::getUserId, userId).last("limit 1").one();
        if (student == null) {
            return Result.success(Map.of("message", "当前账号未关联学生信息"));
        }

        // 成绩列表
        List<Score> scores = scoreService.lambdaQuery()
                .eq(Score::getStudentId, student.getId()).list();

        // 课程信息映射
        Map<Long, Course> courseMap = new HashMap<>();
        if (!scores.isEmpty()) {
            Set<Long> courseIds = new HashSet<>();
            for (Score s : scores) courseIds.add(s.getCourseId());
            courseService.lambdaQuery().in(Course::getId, courseIds).list()
                    .forEach(c -> courseMap.put(c.getId(), c));
        }

        // 组装成绩明细
        List<Map<String, Object>> details = new ArrayList<>();
        double totalScore = 0;
        for (Score s : scores) {
            Course c = courseMap.get(s.getCourseId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("courseName", c != null ? c.getCourseName() : "未知课程");
            item.put("courseCode", c != null ? c.getCourseCode() : "");
            item.put("credit", c != null ? c.getCredit() : 0);
            item.put("score", s.getScore());
            item.put("semester", s.getSemester());
            details.add(item);
            totalScore += s.getScore().doubleValue();
        }

        // 平均分
        double avgScore = scores.isEmpty() ? 0 : totalScore / scores.size();

        // 总学分
        double totalCredit = courseMap.values().stream()
                .mapToDouble(c -> c.getCredit() != null ? c.getCredit().doubleValue() : 0).sum();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("studentNo", student.getStudentNo());
        data.put("name", student.getName());
        data.put("major", student.getMajor());
        data.put("grade", student.getGrade());
        data.put("averageScore", Math.round(avgScore * 10.0) / 10.0);
        data.put("totalCredit", totalCredit);
        data.put("courseCount", courseMap.size());
        data.put("scores", details);

        return Result.success(data);
    }

    private Long getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Claims claims) {
            Long userId = claims.get("userId", Long.class);
            return userId != null ? userId : 1L;
        }
        return 1L;
    }
}
