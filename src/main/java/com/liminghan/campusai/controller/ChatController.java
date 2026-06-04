package com.liminghan.campusai.controller;

import com.liminghan.campusai.common.BusinessException;
import com.liminghan.campusai.common.ErrorCode;
import com.liminghan.campusai.common.Result;
import com.liminghan.campusai.dto.ChatAskRequest;
import com.liminghan.campusai.entity.ChatRecord;
import com.liminghan.campusai.entity.Conversation;
import com.liminghan.campusai.security.SecurityUtils;
import com.liminghan.campusai.service.ChatRecordService;
import com.liminghan.campusai.service.ChatService;
import com.liminghan.campusai.service.ConversationService;
import com.liminghan.campusai.vo.ChatResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "AI 问答", description = "对话式 AI 问答（RAG 辅助）")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatRecordService chatRecordService;
    private final ConversationService conversationService;

    public ChatController(ChatService chatService,
                          ChatRecordService chatRecordService,
                          ConversationService conversationService) {
        this.chatService = chatService;
        this.chatRecordService = chatRecordService;
        this.conversationService = conversationService;
    }

    @Operation(summary = "发送消息（自动创建或继续对话）")
    @PostMapping("/ask")
    public Result<ChatResponseVO> ask(@Valid @RequestBody ChatAskRequest request) {
        if (request.getUserId() == null) {
            populateUserFromJwt(request);
        }
        return Result.success(chatService.ask(request));
    }

    @Operation(summary = "获取对话消息历史（仅限自己的对话）")
    @GetMapping("/messages")
    public Result<List<ChatRecord>> messages(@RequestParam Long conversationId) {
        Long userId = getCurrentUserId();
        // 验证对话属于当前用户
        Conversation conv = conversationService.getById(conversationId);
        if (conv == null || !conv.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "对话不存在或无权访问");
        }
        return Result.success(chatRecordService.lambdaQuery()
                .eq(ChatRecord::getConversationId, conversationId)
                .orderByAsc(ChatRecord::getCreatedAt)
                .list());
    }

    private void populateUserFromJwt(ChatAskRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        request.setUserId(userId);
        request.setUsername(SecurityUtils.getCurrentUsername());
    }

    @Operation(summary = "查询当前用户的全部问答历史（跨对话）")
    @GetMapping("/history")
    public Result<List<ChatRecord>> history() {
        Long userId = getCurrentUserId();
        return Result.success(chatRecordService.lambdaQuery()
                .eq(ChatRecord::getUserId, userId)
                .orderByDesc(ChatRecord::getCreatedAt)
                .last("limit 50")
                .list());
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }
}
