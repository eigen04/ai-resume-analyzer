package com.example.ai_resume_analyzer.dto.interview;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InterviewPrepRequest {
    @NotBlank(message = "Company name cannot be blank")
    private String companyName;

    @NotBlank(message = "Job title cannot be blank")
    private String jobTitle;

    @NotBlank(message = "Job description cannot be blank")
    private String jobDescription;
}
