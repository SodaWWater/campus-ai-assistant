package com.liminghan.campusai.controller;

import com.liminghan.campusai.common.Result;
import com.liminghan.campusai.dto.ChatAskRequest;
import com.liminghan.campusai.service.ChatService;
import com.liminghan.campusai.vo.ChatResponseVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "Ask AI assistant")
    @PostMapping("/ask")
    public Result<ChatResponseVO> ask(@Valid @RequestBody ChatAskRequest request) {
        return Result.success(chatService.ask(request));
    }
}
