package com.example.ai_resume_analyzer.dto;

import java.util.Map;
import java.util.List;

/**
 * Categorized resume improvement suggestions response.
 * Categories include ATS Keywords, Quantified Impact, Projects, Skills, Formatting, Experience.
 */
public class ResumeSuggestionResponse {

    /**
     * Map of category name → list of specific, actionable suggestions.
     * e.g., {"ATS Keywords": ["Add 'Kubernetes' to skills", ...], "Projects": [...]}
     */
    private Map<String, List<String>> suggestions;

    public ResumeSuggestionResponse() {}

    public ResumeSuggestionResponse(Map<String, List<String>> suggestions) {
        this.suggestions = suggestions;
    }

    public Map<String, List<String>> getSuggestions() { return suggestions; }
    public void setSuggestions(Map<String, List<String>> suggestions) { this.suggestions = suggestions; }
}