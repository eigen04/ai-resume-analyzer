package com.example.ai_resume_analyzer.dto.ats;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ResumeTemplateData {
    @JsonProperty("summary_text")
    private String summaryText;
    
    @JsonProperty("skills_languages")
    private String skillsLanguages;
    
    @JsonProperty("skills_frameworks")
    private String skillsFrameworks;
    
    @JsonProperty("skills_db_cloud")
    private String skillsDbCloud;
    
    @JsonProperty("skills_tools")
    private String skillsTools;
    
    @JsonProperty("experience_content")
    private String experienceContent;
    
    @JsonProperty("projects_content")
    private String projectsContent;
    
    @JsonProperty("achievements_content")
    private String achievementsContent;
}
