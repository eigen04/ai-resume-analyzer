package com.example.ai_resume_analyzer.exception;

/**
 * Thrown when a resume is not found for a given ID.
 * Maps to HTTP 404 Not Found.
 */
public class ResumeNotFoundException extends RuntimeException {

    private final Long resumeId;

    public ResumeNotFoundException(Long resumeId) {
        super("Resume not found with id: " + resumeId);
        this.resumeId = resumeId;
    }

    public Long getResumeId() {
        return resumeId;
    }
}
