package com.liminghan.campusai.controller;

import com.liminghan.campusai.common.BusinessException;
import com.liminghan.campusai.common.ErrorCode;
import com.liminghan.campusai.common.Result;
import com.liminghan.campusai.dto.DocumentCreateRequest;
import com.liminghan.campusai.dto.KnowledgeBaseCreateRequest;
import com.liminghan.campusai.entity.KbDocument;
import com.liminghan.campusai.entity.KbDocumentChunk;
import com.liminghan.campusai.entity.KnowledgeBase;
import com.liminghan.campusai.service.KnowledgeBaseService;
import com.liminghan.campusai.util.FileTextExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Tag(name = "知识库管理", description = "知识库 CRUD、文档上传与处理")
@RestController
@RequestMapping("/api")
public class KnowledgeBaseController {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("txt", "md", "pdf", "docx", "doc");

    private final KnowledgeBaseService knowledgeBaseService;
    private final FileTextExtractor fileTextExtractor;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService,
                                   FileTextExtractor fileTextExtractor) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.fileTextExtractor = fileTextExtractor;
    }

    // ─────────────────── 知识库 CRUD ───────────────────

    @Operation(summary = "创建知识库")
    @PostMapping("/kb")
    public Result<KnowledgeBase> create(@Valid @RequestBody KnowledgeBaseCreateRequest request) {
        return Result.success(knowledgeBaseService.createKnowledgeBase(request));
    }

    @Operation(summary = "列出知识库")
    @GetMapping("/kb")
    public Result<List<KnowledgeBase>> list() {
        return Result.success(knowledgeBaseService.listKnowledgeBases());
    }

    @Operation(summary = "获取知识库详情")
    @GetMapping("/kb/{id}")
    public Result<KnowledgeBase> get(@PathVariable Long id) {
        return Result.success(knowledgeBaseService.getKnowledgeBase(id));
    }

    @Operation(summary = "更新知识库")
    @PutMapping("/kb/{id}")
    public Result<KnowledgeBase> update(@PathVariable Long id,
                                        @Valid @RequestBody KnowledgeBaseCreateRequest request) {
        KnowledgeBase kb = knowledgeBaseService.getKnowledgeBase(id);
        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        if (request.getVisibility() != null) {
            kb.setVisibility(request.getVisibility());
        }
        kb.setUpdatedAt(java.time.LocalDateTime.now());
        knowledgeBaseService.updateById(kb);
        return Result.success(kb);
    }

    @Operation(summary = "删除知识库")
    @DeleteMapping("/kb/{id}")
    public Result<String> delete(@PathVariable Long id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return Result.success("ok");
    }

    // ─────────────────── 文档管理 ───────────────────

    @Operation(summary = "录入文档正文（同步处理）")
    @PostMapping("/kb/{knowledgeBaseId}/document")
    public Result<Map<String, Long>> addDocument(@PathVariable Long knowledgeBaseId,
                                                 @Valid @RequestBody DocumentCreateRequest request) {
        Long documentId = knowledgeBaseService.addDocument(knowledgeBaseId, request);
        return Result.success(Map.of("documentId", documentId));
    }

    @Operation(summary = "上传文件（multipart: txt/md/pdf/docx）并异步处理")
    @PostMapping("/kb/{knowledgeBaseId}/document/upload")
    public Result<Map<String, Object>> uploadDocument(@PathVariable Long knowledgeBaseId,
                                                      @RequestParam("file") MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件名为空");
        }

        String fileType = getExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(fileType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "不支持的文件类型: ." + fileType + "，仅支持 txt/md/pdf/docx/doc");
        }

        String content;
        try {
            content = fileTextExtractor.extract(file.getBytes(), fileType);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "读取文件失败: " + e.getMessage());
        }

        if (content.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件内容为空，无法提取文本");
        }

        Long documentId = knowledgeBaseService.uploadDocumentAsync(
                knowledgeBaseId,
                originalFilename,
                fileType,
                file.getSize(),
                content
        );

        return Result.success(Map.of("documentId", documentId, "status", "PROCESSING"));
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }

    @Operation(summary = "列出知识库下的文档")
    @GetMapping("/kb/{knowledgeBaseId}/documents")
    public Result<List<KbDocument>> listDocuments(@PathVariable Long knowledgeBaseId) {
        return Result.success(knowledgeBaseService.listDocuments(knowledgeBaseId));
    }

    @Operation(summary = "查看文档片段")
    @GetMapping("/document/{documentId}/chunks")
    public Result<List<KbDocumentChunk>> listDocumentChunks(@PathVariable Long documentId) {
        return Result.success(knowledgeBaseService.listDocumentChunks(documentId));
    }

    @Operation(summary = "重新解析文档")
    @PostMapping("/document/{documentId}/reprocess")
    public Result<Map<String, String>> reprocessDocument(@PathVariable Long documentId) {
        knowledgeBaseService.reprocessDocument(documentId);
        return Result.success(Map.of("status", "PROCESSING"));
    }

    // ─────────────────── 兼容旧接口 ───────────────────

    @Operation(summary = "列出知识库下所有片段（按知识库）")
    @GetMapping("/kb/{knowledgeBaseId}/chunks")
    public Result<List<KbDocumentChunk>> listChunks(@PathVariable Long knowledgeBaseId) {
        return Result.success(knowledgeBaseService.listChunks(knowledgeBaseId));
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/kb/document/{documentId}")
    public Result<String> deleteDocument(@PathVariable Long documentId) {
        knowledgeBaseService.deleteDocument(documentId);
        return Result.success("ok");
    }
}
