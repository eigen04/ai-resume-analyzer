package com.example.ai_resume_analyzer.dto.interview;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class InterviewRound {
    @JsonProperty("round_name")
    private String roundName;

    @JsonProperty("estimated_difficulty")
    private String estimatedDifficulty;

    @JsonProperty("questions")
    private List<String> questions;

    @JsonProperty("source_links")
    private List<String> sourceLinks;
}
