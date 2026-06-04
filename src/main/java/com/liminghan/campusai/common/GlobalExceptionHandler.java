package com.liminghan.campusai.common;

import jakarta.validation.ConstraintViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException exception) {
        return Result.fail(exception.getErrorCode().getCode(), exception.getMessage());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    public Result<Void> handleValidationException(Exception exception) {
        return Result.fail(ErrorCode.PARAM_ERROR.getCode(), exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDenied(AccessDeniedException exception) {
        return Result.fail(ErrorCode.FORBIDDEN.getCode(),
                exception.getMessage() != null ? exception.getMessage() : "权限不足");
    }

    @ExceptionHandler(AuthenticationException.class)
    public Result<Void> handleAuthentication(AuthenticationException exception) {
        return Result.fail(ErrorCode.UNAUTHORIZED.getCode(),
                exception.getMessage() != null ? exception.getMessage() : "未登录或登录已过期");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        return Result.fail(ErrorCode.SYSTEM_ERROR.getCode(), exception.getMessage());
    }
}
