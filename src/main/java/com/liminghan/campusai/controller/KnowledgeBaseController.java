package com.liminghan.campusai.controller;

import com.liminghan.campusai.common.Result;
import com.liminghan.campusai.dto.DocumentCreateRequest;
import com.liminghan.campusai.dto.KnowledgeBaseCreateRequest;
import com.liminghan.campusai.entity.KbDocumentChunk;
import com.liminghan.campusai.entity.KnowledgeBase;
import com.liminghan.campusai.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kb")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Operation(summary = "Create knowledge base")
    @PostMapping
    public Result<KnowledgeBase> create(@Valid @RequestBody KnowledgeBaseCreateRequest request) {
        return Result.success(knowledgeBaseService.createKnowledgeBase(request));
    }

    @Operation(summary = "List knowledge bases")
    @GetMapping("/list")
    public Result<List<KnowledgeBase>> list() {
        return Result.success(knowledgeBaseService.listKnowledgeBases());
    }

    @Operation(summary = "Get knowledge base")
    @GetMapping("/{id}")
    public Result<KnowledgeBase> get(@PathVariable Long id) {
        return Result.success(knowledgeBaseService.getKnowledgeBase(id));
    }

    @Operation(summary = "Delete knowledge base")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return Result.success("ok");
    }

    @Operation(summary = "Add document to knowledge base")
    @PostMapping("/{knowledgeBaseId}/document")
    public Result<Map<String, Long>> addDocument(@PathVariable Long knowledgeBaseId,
                                                 @Valid @RequestBody DocumentCreateRequest request) {
        Long documentId = knowledgeBaseService.addDocument(knowledgeBaseId, request);
        return Result.success(Map.of("documentId", documentId));
    }

    @Operation(summary = "List chunks in knowledge base")
    @GetMapping("/{knowledgeBaseId}/chunks")
    public Result<List<KbDocumentChunk>> listChunks(@PathVariable Long knowledgeBaseId) {
        return Result.success(knowledgeBaseService.listChunks(knowledgeBaseId));
    }

    @Operation(summary = "Delete document")
    @DeleteMapping("/document/{documentId}")
    public Result<String> deleteDocument(@PathVariable Long documentId) {
        knowledgeBaseService.deleteDocument(documentId);
        return Result.success("ok");
    }
}
