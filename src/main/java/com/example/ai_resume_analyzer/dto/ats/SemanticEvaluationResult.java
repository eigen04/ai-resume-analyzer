package com.example.ai_resume_analyzer.dto.ats;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SemanticEvaluationResult {
    @JsonProperty("semantic_score")
    private int semanticScore; // Out of 100
    
    @JsonProperty("critique")
    private String critique;
}
