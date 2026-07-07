package com.example.ai_resume_analyzer.dto;

import jakarta.validation.constraints.NotBlank;

public class JobMatchRequest {

    @NotBlank(message = "jobDescription must not be blank")
    private String jobDescription;

    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }
}