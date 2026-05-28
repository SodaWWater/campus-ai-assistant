package com.liminghan.campusai.controller;

import com.liminghan.campusai.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Operation(summary = "Health check")
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("ok");
    }
}
