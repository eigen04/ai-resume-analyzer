package com.example.ai_resume_analyzer.dto;

import java.util.List;

/**
 * Response for the resume Q&A endpoint.
 * Includes the LLM answer and the source chunks that supported it.
 */
public class AskResumeResponse {

    private Long resumeId;
    private String answer;

    /** The resume text chunks that were retrieved and used to generate the answer. */
    private List<String> sources;

    public AskResumeResponse() {}

    public AskResumeResponse(Long resumeId, String answer, List<String> sources) {
        this.resumeId = resumeId;
        this.answer = answer;
        this.sources = sources;
    }

    public Long getResumeId() { return resumeId; }
    public void setResumeId(Long resumeId) { this.resumeId = resumeId; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }
}