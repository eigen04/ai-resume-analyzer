package com.example.ai_resume_analyzer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Standard error response envelope for all API error conditions.
 * Returned by GlobalExceptionHandler for every error case.
 */
public record ApiErrorResponse(
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime timestamp,
    int status,
    String errorCode,
    String message,
    String path
) {
    public ApiErrorResponse(int status, String errorCode, String message, String path) {
        this(LocalDateTime.now(), status, errorCode, message, path);
    }
}
