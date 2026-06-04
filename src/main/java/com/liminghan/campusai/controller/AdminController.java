package com.liminghan.campusai.controller;

import com.liminghan.campusai.common.BusinessException;
import com.liminghan.campusai.common.ErrorCode;
import com.liminghan.campusai.common.Result;
import com.liminghan.campusai.entity.KbDocument;
import com.liminghan.campusai.entity.KnowledgeBase;
import com.liminghan.campusai.entity.Student;
import com.liminghan.campusai.entity.SysUser;
import com.liminghan.campusai.security.SecurityUtils;
import com.liminghan.campusai.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "管理员端", description = "系统看板、用户管理、知识库审计、文档任务")
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final SysUserService sysUserService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final KbDocumentService documentService;
    private final KbDocumentChunkService chunkService;
    private final ChatRecordService chatRecordService;
    private final StudentService studentService;
    private final PasswordEncoder passwordEncoder;

    public AdminController(SysUserService sysUserService,
                           KnowledgeBaseService knowledgeBaseService,
                           KbDocumentService documentService,
                           KbDocumentChunkService chunkService,
                           ChatRecordService chatRecordService,
                           StudentService studentService,
                           PasswordEncoder passwordEncoder) {
        this.sysUserService = sysUserService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentService = documentService;
        this.chunkService = chunkService;
        this.chatRecordService = chatRecordService;
        this.studentService = studentService;
        this.passwordEncoder = passwordEncoder;
    }

    // ═══════════════════ Dashboard ═══════════════════

    @Operation(summary = "管理员看板")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userCount", sysUserService.count());
        data.put("kbCount", knowledgeBaseService.count());
        long docCount = documentService.count();
        data.put("docCount", docCount);
        data.put("chunkCount", chunkService.count());
        data.put("chatCount", chatRecordService.count());
        long processingCount = documentService.lambdaQuery().eq(KbDocument::getStatus, "PROCESSING").count();
        long doneCount = documentService.lambdaQuery().eq(KbDocument::getStatus, "DONE").count();
        long failedCount = documentService.lambdaQuery().eq(KbDocument::getStatus, "FAILED").count();
        data.put("processingCount", processingCount);
        data.put("doneCount", doneCount);
        data.put("failedCount", failedCount);
        return Result.success(data);
    }

    // ═══════════════════ 用户管理 ═══════════════════

    @Operation(summary = "用户列表")
    @GetMapping("/users")
    public Result<List<SysUser>> users() {
        return Result.success(sysUserService.lambdaQuery()
                .orderByAsc(SysUser::getCreatedAt).list());
    }

    @Operation(summary = "创建用户")
    @PostMapping("/users")
    public Result<SysUser> createUser(@RequestBody Map<String, Object> body) {
        String username = (String) body.get("username");
        String password = (String) body.get("password");
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名和密码不能为空");
        }
        // 检查重名
        long exists = sysUserService.lambdaQuery().eq(SysUser::getUsername, username).count();
        if (exists > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname((String) body.getOrDefault("nickname", username));
        user.setRole((String) body.getOrDefault("role", "STUDENT"));
        user.setStatus((String) body.getOrDefault("status", "ENABLED"));
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        sysUserService.save(user);
        // 同步到学生表
        if ("STUDENT".equals(user.getRole())) {
            syncStudent(user);
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @Operation(summary = "编辑用户")
    @PutMapping("/users/{id}")
    public Result<SysUser> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        SysUser user = sysUserService.getById(id);
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        if (body.containsKey("nickname")) user.setNickname((String) body.get("nickname"));
        if (body.containsKey("role")) user.setRole((String) body.get("role"));
        if (body.containsKey("status")) user.setStatus((String) body.get("status"));
        if (body.containsKey("password")) {
            String pwd = (String) body.get("password");
            if (pwd != null && !pwd.isBlank()) {
                user.setPassword(passwordEncoder.encode(pwd));
            }
        }
        user.setUpdatedAt(LocalDateTime.now());
        sysUserService.updateById(user);
        // 如果改为学生角色，同步学生表
        if ("STUDENT".equals(user.getRole())) {
            syncStudent(user);
        }
        user.setPassword(null);
        return Result.success(user);
    }

    @Operation(summary = "删除用户（不能删自己）")
    @DeleteMapping("/users/{id}")
    public Result<String> deleteUser(@PathVariable Long id) {
        Long self = SecurityUtils.getCurrentUserId();
        if (self.equals(id)) throw new BusinessException(ErrorCode.PARAM_ERROR, "不能删除自己");
        SysUser user = sysUserService.getById(id);
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        // 同步删除学生表记录
        if ("STUDENT".equals(user.getRole())) {
            studentService.lambdaUpdate().eq(Student::getUserId, id).remove();
        }
        sysUserService.removeById(id);
        return Result.success("ok");
    }

    @Operation(summary = "切换用户启用/禁用")
    @PutMapping("/users/{id}/status")
    public Result<SysUser> toggleUserStatus(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        user.setStatus("ENABLED".equals(user.getStatus()) ? "DISABLED" : "ENABLED");
        user.setUpdatedAt(LocalDateTime.now());
        sysUserService.updateById(user);
        user.setPassword(null);
        return Result.success(user);
    }

    // ═══════════════════ 知识库审计 ═══════════════════

    @Operation(summary = "全部知识库（审计）")
    @GetMapping("/knowledge-bases")
    public Result<List<KnowledgeBase>> knowledgeBases() {
        return Result.success(knowledgeBaseService.listKnowledgeBases());
    }

    @Operation(summary = "管理员编辑知识库")
    @PutMapping("/knowledge-bases/{id}")
    public Result<KnowledgeBase> updateKb(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        KnowledgeBase kb = knowledgeBaseService.getKnowledgeBase(id);
        if (body.containsKey("name")) kb.setName((String) body.get("name"));
        if (body.containsKey("description")) kb.setDescription((String) body.get("description"));
        if (body.containsKey("visibility")) kb.setVisibility((String) body.get("visibility"));
        kb.setUpdatedAt(LocalDateTime.now());
        knowledgeBaseService.updateById(kb);
        return Result.success(kb);
    }

    @Operation(summary = "管理员删除知识库")
    @DeleteMapping("/knowledge-bases/{id}")
    public Result<String> deleteKb(@PathVariable Long id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return Result.success("ok");
    }

    // ═══════════════════ 文档任务 ═══════════════════

    @Operation(summary = "全部文档列表（含状态）")
    @GetMapping("/documents")
    public Result<List<KbDocument>> documents() {
        return Result.success(documentService.lambdaQuery()
                .orderByDesc(KbDocument::getCreatedAt).list());
    }

    @Operation(summary = "管理员重新解析文档")
    @PostMapping("/document/{documentId}/reprocess")
    public Result<Map<String, String>> reprocessDocument(@PathVariable Long documentId) {
        knowledgeBaseService.reprocessDocument(documentId);
        return Result.success(Map.of("status", "PROCESSING"));
    }

    @Operation(summary = "管理员删除文档")
    @DeleteMapping("/document/{documentId}")
    public Result<String> deleteDocument(@PathVariable Long documentId) {
        knowledgeBaseService.deleteDocument(documentId);
        return Result.success("ok");
    }

    /**
     * 同步创建学生表记录（自动生成学号）
     */
    private void syncStudent(SysUser user) {
        // 检查是否已有关联
        long exists = studentService.lambdaQuery().eq(Student::getUserId, user.getId()).count();
        if (exists > 0) return;
        // 自动生成学号
        String studentNo = "2026" + String.format("%04d", user.getId());
        Student s = new Student();
        s.setUserId(user.getId());
        s.setStudentNo(studentNo);
        s.setName(user.getNickname() != null ? user.getNickname() : user.getUsername());
        s.setMajor("");
        s.setGrade("");
        studentService.save(s);
    }

}
