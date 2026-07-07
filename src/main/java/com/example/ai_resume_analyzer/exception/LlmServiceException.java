package com.example.ai_resume_analyzer.exception;

/**
 * Thrown when the LLM provider is unavailable, returns an error, or returns
 * a response that cannot be parsed into the expected format.
 * Maps to HTTP 502 Bad Gateway (upstream AI provider failure).
 */
public class LlmServiceException extends RuntimeException {

    public LlmServiceException(String message) {
        super(message);
    }

    public LlmServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
