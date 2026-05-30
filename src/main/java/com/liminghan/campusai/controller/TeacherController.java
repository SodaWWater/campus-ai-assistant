package com.liminghan.campusai.controller;

import com.liminghan.campusai.common.Result;
import com.liminghan.campusai.entity.KbDocument;
import com.liminghan.campusai.entity.KnowledgeBase;
import com.liminghan.campusai.service.KbDocumentService;
import com.liminghan.campusai.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "教师端", description = "教师工作台、知识库管理")
@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KbDocumentService documentService;

    public TeacherController(KnowledgeBaseService knowledgeBaseService,
                             KbDocumentService documentService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentService = documentService;
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
}
