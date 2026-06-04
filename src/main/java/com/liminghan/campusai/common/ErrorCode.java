package com.liminghan.campusai.common;

public enum ErrorCode {
    SUCCESS(0, "success"),
    PARAM_ERROR(400, "request parameter error"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "resource not found"),
    SYSTEM_ERROR(500, "system error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
