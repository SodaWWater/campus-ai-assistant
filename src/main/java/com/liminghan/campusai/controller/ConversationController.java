package com.liminghan.campusai.controller;

import com.liminghan.campusai.common.BusinessException;
import com.liminghan.campusai.common.ErrorCode;
import com.liminghan.campusai.common.Result;
import com.liminghan.campusai.entity.Conversation;
import com.liminghan.campusai.security.SecurityUtils;
import com.liminghan.campusai.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "对话管理", description = "创建、列出、删除对话")
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @Operation(summary = "获取当前用户的对话列表")
    @GetMapping
    public Result<List<Conversation>> list() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(conversationService.lambdaQuery()
                .eq(Conversation::getUserId, userId)
                .orderByDesc(Conversation::getUpdatedAt)
                .list());
    }

    @Operation(summary = "创建新对话")
    @PostMapping
    public Result<Conversation> create(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        Conversation conv = new Conversation();
        conv.setUserId(userId);
        conv.setTitle(body.get("title") != null ? body.get("title").toString() : "新对话");
        Object kbId = body.get("knowledgeBaseId");
        if (kbId != null) {
            conv.setKnowledgeBaseId(Long.valueOf(kbId.toString()));
        }
        conv.setCreatedAt(LocalDateTime.now());
        conv.setUpdatedAt(LocalDateTime.now());
        conversationService.save(conv);
        return Result.success(conv);
    }

    @Operation(summary = "删除对话（仅限自己的）")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Conversation conv = conversationService.getById(id);
        if (conv == null || !conv.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "对话不存在或无权删除");
        }
        conversationService.removeById(id);
        return Result.success("ok");
    }

}
