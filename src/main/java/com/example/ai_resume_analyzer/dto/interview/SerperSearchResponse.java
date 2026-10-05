package com.example.ai_resume_analyzer.dto.interview;

import lombok.Data;
import java.util.List;

@Data
public class SerperSearchResponse {
    private List<OrganicResult> organic;

    @Data
    public static class OrganicResult {
        private String title;
        private String link;
        private String snippet;
    }
}
