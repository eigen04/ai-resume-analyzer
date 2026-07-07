package com.example.ai_resume_analyzer.exception;

/**
 * Thrown when embedding generation or vector store operations fail.
 * Maps to HTTP 500 Internal Server Error.
 */
public class EmbeddingException extends RuntimeException {

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}
