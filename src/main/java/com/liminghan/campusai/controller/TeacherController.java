package com.liminghan.campusai.controller;

import com.liminghan.campusai.common.Result;
import com.liminghan.campusai.entity.ChatRecord;
import com.liminghan.campusai.entity.KbDocument;
import com.liminghan.campusai.entity.KnowledgeBase;
import com.liminghan.campusai.service.ChatRecordService;
import com.liminghan.campusai.service.KbDocumentService;
import com.liminghan.campusai.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "教师端", description = "教师工作台、知识库管理、问题分析")
@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KbDocumentService documentService;
    private final ChatRecordService chatRecordService;

    public TeacherController(KnowledgeBaseService knowledgeBaseService,
                             KbDocumentService documentService,
                             ChatRecordService chatRecordService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentService = documentService;
        this.chatRecordService = chatRecordService;
    }

    @Operation(summary = "教师工作台数据")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        List<KnowledgeBase> kbs = knowledgeBaseService.listKnowledgeBases();

        int docCount = 0;
        int processingCount = 0;
        int failedCount = 0;
        for (KnowledgeBase kb : kbs) {
            List<KbDocument> docs = knowledgeBaseService.listDocuments(kb.getId());
            docCount += docs.size();
            processingCount += docs.stream().filter(d -> "PROCESSING".equals(d.getStatus())).count();
            failedCount += docs.stream().filter(d -> "FAILED".equals(d.getStatus())).count();
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("kbCount", kbs.size());
        data.put("docCount", docCount);
        data.put("processingCount", processingCount);
        data.put("failedCount", failedCount);
        data.put("knowledgeBases", kbs);
        return Result.success(data);
    }

    @Operation(summary = "教师自己的知识库")
    @GetMapping("/knowledge-bases")
    public Result<List<KnowledgeBase>> knowledgeBases() {
        return Result.success(knowledgeBaseService.listKnowledgeBases());
    }

    @Operation(summary = "学生问题分析")
    @GetMapping("/question-analytics")
    public Result<Map<String, Object>> questionAnalytics() {
        List<ChatRecord> records = chatRecordService.lambdaQuery()
                .orderByDesc(ChatRecord::getCreatedAt)
                .last("limit 100")
                .list();

        long ragCount = records.stream().filter(r -> "GENERAL_RAG".equals(r.getSourceType()) || "RAG".equals(r.getSourceType())).count();
        long academicCount = records.stream().filter(r -> r.getSourceType() != null && r.getSourceType().contains("ACADEMIC")).count();
        long noCitationCount = records.stream()
                .filter(r -> r.getMatchedChunkIds() == null || r.getMatchedChunkIds().isBlank())
                .count();

        double avgRetrieval = records.stream()
                .mapToLong(r -> r.getRetrievalTimeMs() == null ? 0 : r.getRetrievalTimeMs())
                .average()
                .orElse(0);
        double avgGeneration = records.stream()
                .mapToLong(r -> r.getGenerationTimeMs() == null ? 0 : r.getGenerationTimeMs())
                .average()
                .orElse(0);

        List<Map<String, Object>> topQuestions = records.stream()
                .collect(Collectors.groupingBy(ChatRecord::getQuestion, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(8)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("question", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalCount", records.size());
        data.put("ragCount", ragCount);
        data.put("academicCount", academicCount);
        data.put("noCitationCount", noCitationCount);
        data.put("avgRetrievalTimeMs", Math.round(avgRetrieval));
        data.put("avgGenerationTimeMs", Math.round(avgGeneration));
        data.put("topQuestions", topQuestions);
        data.put("records", records);
        return Result.success(data);
    }
}
