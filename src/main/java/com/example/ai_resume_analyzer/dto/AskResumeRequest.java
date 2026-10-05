package com.example.ai_resume_analyzer.dto;

import jakarta.validation.constraints.NotBlank;

public class AskResumeRequest {

    @NotBlank(message = "question must not be blank")
    private String question;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
}