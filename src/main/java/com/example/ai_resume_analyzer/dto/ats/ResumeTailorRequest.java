package com.example.ai_resume_analyzer.dto.ats;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResumeTailorRequest {
    @NotBlank(message = "Job description cannot be blank")
    private String jobDescription;
    
    @NotBlank(message = "Role cannot be blank")
    private String role;
    
    // User basic info for template injection
    private String candidateName;
    private String email;
    private String phone;
    private String linkedinUrl;
    private String githubUrl;
    private String portfolioUrl;
    
    // Education info (usually remains static, just passed through)
    private String educationUniversity;
    private String educationLocation;
    private String educationDegree;
    private String educationCgpa;
    private String educationDuration;
}
