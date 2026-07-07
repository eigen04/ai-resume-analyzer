package com.example.ai_resume_analyzer.handler;

import com.example.ai_resume_analyzer.dto.ApiErrorResponse;
import com.example.ai_resume_analyzer.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.AccessDeniedException;

import java.util.stream.Collectors;

/**
 * Centralized exception handler for all REST controllers.
 *
 * Replaces repeated try-catch blocks in individual controllers with a single,
 * consistent error-handling strategy. Every error returns an ApiErrorResponse
 * with a stable, machine-readable errorCode.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResumeNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResumeNotFound(
            ResumeNotFoundException ex, HttpServletRequest request) {
        log.warn("Resume not found: resumeId={}", ex.getResumeId());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            new ApiErrorResponse(404, "RESUME_NOT_FOUND", ex.getMessage(), request.getRequestURI())
        );
    }

    @ExceptionHandler(DuplicateResumeException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateResume(
            DuplicateResumeException ex, HttpServletRequest request) {
        log.warn("Duplicate resume upload detected: existingId={}", ex.getExistingResumeId());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            new ApiErrorResponse(409, "DUPLICATE_RESUME", ex.getMessage(), request.getRequestURI())
        );
    }

    @ExceptionHandler(InvalidPdfException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPdf(
            InvalidPdfException ex, HttpServletRequest request) {
        log.warn("Invalid PDF upload: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ApiErrorResponse(400, "INVALID_PDF", ex.getMessage(), request.getRequestURI())
        );
    }

    @ExceptionHandler(EmbeddingException.class)
    public ResponseEntity<ApiErrorResponse> handleEmbedding(
            EmbeddingException ex, HttpServletRequest request) {
        log.error("Embedding service failure: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ApiErrorResponse(500, "EMBEDDING_FAILURE", "Failed to process resume embeddings. Please try again.", request.getRequestURI())
        );
    }

    @ExceptionHandler(LlmServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleLlmService(
            LlmServiceException ex, HttpServletRequest request) {
        log.error("LLM service failure: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(
            new ApiErrorResponse(502, "LLM_SERVICE_UNAVAILABLE", "AI analysis service is temporarily unavailable. Please try again later.", request.getRequestURI())
        );
    }

    @ExceptionHandler(ResumeProcessingException.class)
    public ResponseEntity<ApiErrorResponse> handleProcessing(
            ResumeProcessingException ex, HttpServletRequest request) {
        log.error("Resume processing failure: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ApiErrorResponse(500, "PROCESSING_FAILURE", "Resume processing failed. Please try again.", request.getRequestURI())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String details = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining("; "));
        log.warn("Request validation failed: {}", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ApiErrorResponse(400, "VALIDATION_ERROR", details, request.getRequestURI())
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleFileSizeLimit(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("File upload size limit exceeded");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ApiErrorResponse(400, "FILE_TOO_LARGE", "Uploaded file exceeds the maximum allowed size of 10MB.", request.getRequestURI())
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed or missing request body");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            new ApiErrorResponse(400, "MALFORMED_REQUEST", "Request body is missing or malformed.", request.getRequestURI())
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest request) {
        log.warn("API request failed with status: {}, reason: {}", ex.getStatusCode(), ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(
            new ApiErrorResponse(ex.getStatusCode().value(), "API_ERROR", ex.getReason(), request.getRequestURI())
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            new ApiErrorResponse(403, "FORBIDDEN", ex.getMessage(), request.getRequestURI())
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            new ApiErrorResponse(500, "INTERNAL_ERROR", "An unexpected error occurred. Please contact support.", request.getRequestURI())
        );
    }
}
