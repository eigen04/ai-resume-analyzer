package com.example.ai_resume_analyzer.dto;

import com.example.ai_resume_analyzer.entity.ResumeStatus;

import java.time.LocalDateTime;

/**
 * Lightweight DTO for listing resumes without the full extracted text payload.
 */
public class ResumeDto {

    private Long id;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private ResumeStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ResumeDto() {}

    public ResumeDto(Long id, String fileName, String contentType, Long fileSize,
                     ResumeStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public String getContentType() { return contentType; }
    public Long getFileSize() { return fileSize; }
    public ResumeStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public void setStatus(ResumeStatus status) { this.status = status; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
