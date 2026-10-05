package com.example.ai_resume_analyzer.dto.ats;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class KeywordExtractionResult {
    @JsonProperty("keywords")
    private List<String> keywords;
}
