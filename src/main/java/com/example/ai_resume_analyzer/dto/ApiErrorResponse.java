package com.example.ai_resume_analyzer.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Standard error response envelope for all API error conditions.
 * Returned by GlobalExceptionHandler for every error case.
 */
public class ApiErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private int status;
    private String errorCode;
    private String message;
    private String path;

    public ApiErrorResponse() {}

    public ApiErrorResponse(int status, String errorCode, String message, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.path = path;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public String getMessage() { return message; }
    public String getPath() { return path; }

    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setStatus(int status) { this.status = status; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public void setMessage(String message) { this.message = message; }
    public void setPath(String path) { this.path = path; }
}
