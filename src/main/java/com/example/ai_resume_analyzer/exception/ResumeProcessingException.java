package com.example.ai_resume_analyzer.exception;

/**
 * Thrown when the resume processing pipeline fails (chunking, embedding, etc.).
 * Maps to HTTP 500 Internal Server Error.
 */
public class ResumeProcessingException extends RuntimeException {

    public ResumeProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
