package com.example.bankcards.dto.response;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private Integer status;
    private String message;
    private Map<String, String> fieldErrors;
    private LocalDateTime timestamp;

    public static ErrorResponse of(String error, Integer status, String message) {
        return ErrorResponse.builder()
                .error(error)
                .status(status)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static ErrorResponse validation(String message, Map<String, String> fieldErrors) {
        return ErrorResponse.builder()
                .error("VALIDATION_ERROR")
                .status(400)
                .message(message)
                .fieldErrors(fieldErrors)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
