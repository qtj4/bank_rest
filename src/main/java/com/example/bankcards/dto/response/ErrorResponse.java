package com.example.bankcards.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        String error,
        Integer status,
        String message,
        Map<String, String> fieldErrors,
        LocalDateTime timestamp
) {

    public static ErrorResponse of(String error, Integer status, String message) {
        return new ErrorResponse(error, status, message, null, LocalDateTime.now());
    }

    public static ErrorResponse validation(String message, Map<String, String> fieldErrors) {
        return new ErrorResponse("VALIDATION_ERROR", 400, message, fieldErrors, LocalDateTime.now());
    }
}
