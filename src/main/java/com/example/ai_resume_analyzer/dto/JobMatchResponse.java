package com.example.ai_resume_analyzer.dto;

import java.util.List;

/**
 * Structured response for resume-to-job-description matching.
 * Contains both a quantitative score and qualitative LLM-generated analysis.
 */
public class JobMatchResponse {

    /** Overall match score (0-100), computed via hybrid keyword + semantic analysis. */
    private int matchScore;

    /** Skills/keywords found in both the resume and job description. */
    private List<String> matchedSkills;

    /** Skills/keywords required by the JD but absent from the resume. */
    private List<String> missingSkills;

    /** Resume strengths relevant to this specific job. */
    private List<String> strengths;

    /** Resume weaknesses or gaps relative to this job. */
    private List<String> weaknesses;

    /** Concrete actions the candidate can take to improve their match score. */
    private List<String> recommendations;

    /** One-paragraph narrative summary of the match analysis. */
    private String summary;

    public JobMatchResponse() {}

    public int getMatchScore() { return matchScore; }
    public void setMatchScore(int matchScore) { this.matchScore = matchScore; }

    public List<String> getMatchedSkills() { return matchedSkills; }
    public void setMatchedSkills(List<String> matchedSkills) { this.matchedSkills = matchedSkills; }

    public List<String> getMissingSkills() { return missingSkills; }
    public void setMissingSkills(List<String> missingSkills) { this.missingSkills = missingSkills; }

    public List<String> getStrengths() { return strengths; }
    public void setStrengths(List<String> strengths) { this.strengths = strengths; }

    public List<String> getWeaknesses() { return weaknesses; }
    public void setWeaknesses(List<String> weaknesses) { this.weaknesses = weaknesses; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}