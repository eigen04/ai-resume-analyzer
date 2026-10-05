package com.example.ai_resume_analyzer.dto.interview;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class InterviewPrepGuide {
    @JsonProperty("overview")
    private String overview;

    @JsonProperty("rounds")
    private List<InterviewRound> rounds;
}
