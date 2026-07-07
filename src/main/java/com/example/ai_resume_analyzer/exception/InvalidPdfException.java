package com.example.ai_resume_analyzer.exception;

/**
 * Thrown when an uploaded file is not a valid processable PDF.
 * Covers cases like: non-PDF file, encrypted PDF, image-based/scanned PDF, empty PDF.
 * Maps to HTTP 400 Bad Request.
 */
public class InvalidPdfException extends RuntimeException {

    public InvalidPdfException(String message) {
        super(message);
    }

    public InvalidPdfException(String message, Throwable cause) {
        super(message, cause);
    }
}
