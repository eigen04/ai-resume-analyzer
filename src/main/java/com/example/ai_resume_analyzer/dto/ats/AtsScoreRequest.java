package com.example.ai_resume_analyzer.dto.ats;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AtsScoreRequest {
    @NotBlank(message = "Job description cannot be blank")
    private String jobDescription;
    
    @NotBlank(message = "Role cannot be blank")
    private String role;
    
    private String yearsOfExperience;
    private String companyName;
}
