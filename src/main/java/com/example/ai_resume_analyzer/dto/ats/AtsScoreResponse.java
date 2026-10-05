package com.example.ai_resume_analyzer.dto.ats;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AtsScoreResponse {
    private int finalScore; // Out of 100
    private double keywordMatchScore; // Out of 40
    private double semanticScore; // Out of 60
    
    private List<String> matchedKeywords;
    private List<String> missingKeywords;
    
    private String llmCritique;
}
