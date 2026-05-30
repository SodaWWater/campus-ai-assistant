package com.liminghan.campusai.controller;

import com.liminghan.campusai.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "系统", description = "系统配置与状态")
@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final DataSource dataSource;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${llm.mode:mock}")
    private String llmMode;

    @Value("${llm.provider:deepseek}")
    private String llmProvider;

    public SystemController(DataSource dataSource,
                            RedisTemplate<String, Object> redisTemplate) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
    }

    @Operation(summary = "系统配置与状态")
    @GetMapping("/config")
    public Result<Map<String, Object>> config() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("llmMode", llmMode);
        data.put("provider", llmProvider);

        // MySQL 状态
        try (Connection conn = dataSource.getConnection()) {
            data.put("mysql", conn.isValid(2));
        } catch (Exception e) {
            data.put("mysql", false);
        }

        // Redis 状态
        try {
            redisTemplate.opsForValue().get("health:check");
            data.put("redis", true);
        } catch (Exception e) {
            data.put("redis", false);
        }

        // RabbitMQ 状态（简化：假设配置了就是可用的）
        data.put("rabbitmq", true);

        return Result.success(data);
    }

    @Operation(summary = "系统概览")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return config();
    }
}
