package com.example.ai_resume_analyzer.exception;

/**
 * Thrown when a duplicate resume file is detected via SHA-256 hash comparison.
 * Maps to HTTP 409 Conflict.
 */
public class DuplicateResumeException extends RuntimeException {

    private final Long existingResumeId;

    public DuplicateResumeException(Long existingResumeId) {
        super("A resume with the same content already exists with id: " + existingResumeId);
        this.existingResumeId = existingResumeId;
    }

    public Long getExistingResumeId() {
        return existingResumeId;
    }
}
