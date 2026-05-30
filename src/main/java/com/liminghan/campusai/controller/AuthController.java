package com.liminghan.campusai.controller;

import com.liminghan.campusai.common.Result;
import com.liminghan.campusai.dto.LoginRequest;
import com.liminghan.campusai.service.AuthService;
import com.liminghan.campusai.vo.LoginResponse;
import io.jsonwebtoken.Claims;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Tag(name = "认证", description = "登录与当前用户")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "登录", description = "使用用户名密码登录，返回 JWT Token")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.getUsername(), request.getPassword());
        return Result.success(response);
    }

    @Operation(summary = "当前用户", description = "获取当前登录用户信息")
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Result.success(Map.of("status", "anonymous"));
        }
        Object details = auth.getDetails();
        if (details instanceof Claims claims) {
            Map<String, Object> info = new HashMap<>();
            info.put("userId", claims.get("userId", Long.class));
            info.put("username", claims.getSubject());
            info.put("role", claims.get("role", String.class));
            return Result.success(info);
        }
        return Result.success(Map.of("username", auth.getName()));
    }
}
