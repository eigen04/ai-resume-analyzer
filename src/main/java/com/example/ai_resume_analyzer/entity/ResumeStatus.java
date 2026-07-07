package com.example.ai_resume_analyzer.entity;

/**
 * Lifecycle status of a resume through the processing pipeline.
 */
public enum ResumeStatus {

    /** File has been received but not yet processed. */
    UPLOADED,

    /** Text has been extracted from the PDF. */
    TEXT_EXTRACTED,

    /** Text chunks have been embedded and stored in the vector store. */
    EMBEDDED,

    /** Processing failed at some stage. */
    FAILED
}
