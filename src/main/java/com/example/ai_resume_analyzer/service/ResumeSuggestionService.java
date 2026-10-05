package com.example.ai_resume_analyzer.service;

import com.example.ai_resume_analyzer.config.PromptTemplates;
import com.example.ai_resume_analyzer.dto.ResumeSuggestionResponse;
import com.example.ai_resume_analyzer.entity.Resume;
import com.example.ai_resume_analyzer.exception.LlmServiceException;
import com.example.ai_resume_analyzer.util.LlmResponseUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Generates categorized, specific improvement suggestions for a resume using LLM.
 *
 * Suggestions are organized by category (ATS Keywords, Projects, Skills, etc.)
 * and grounded in the actual resume content. An optional job description can be
 * provided to make suggestions targeted to a specific role.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeSuggestionService {

    private final ResumeService resumeService;
    private final LlmClientService llmClientService;
    private final ObjectMapper objectMapper;

    /**
     * Generates categorized improvement suggestions for a resume.
     *
     * @param resumeId       the resume to analyze
     * @param jobDescription optional job description for targeted suggestions
     * @return categorized suggestions map
     * @throws LlmServiceException if the LLM call fails and cannot be recovered
     */
    public ResumeSuggestionResponse generateSuggestions(Long resumeId, String jobDescription) {
        Resume resume = resumeService.getById(resumeId);
        String resumeText = resume.getExtractedText();

        if (resumeText == null || resumeText.isBlank()) {
            throw new LlmServiceException("Resume text has not been extracted yet for resumeId: " + resumeId);
        }

        log.info("Generating suggestions: resumeId={}, hasJobDescription={}", resumeId, jobDescription != null);

        // Build optional JD context section
        String jdContext = (jobDescription != null && !jobDescription.isBlank())
            ? "Target Job Description (tailor suggestions to this role):\n---\n" + jobDescription + "\n---"
            : "No specific job description provided. Provide general software engineering best practices.";

        String userPrompt = PromptTemplates.SUGGESTIONS_USER
            .replace("{resumeText}", resumeText.substring(0, Math.min(resumeText.length(), 4000)))
            .replace("{jobDescriptionContext}", jdContext);

        String rawResponse = llmClientService.complete(PromptTemplates.SUGGESTIONS_SYSTEM, userPrompt);
        String cleaned = LlmResponseUtils.stripCodeFences(rawResponse);

        try {
            Map<String, List<String>> categorized = objectMapper.readValue(
                cleaned,
                new TypeReference<Map<String, List<String>>>() {}
            );
            log.info("Suggestions generated: resumeId={}, categories={}", resumeId, categorized.keySet());
            return new ResumeSuggestionResponse(categorized);
        } catch (Exception e) {
            log.error("Failed to parse LLM suggestions for resumeId={}: rawResponse={}", resumeId, cleaned, e);
            throw new LlmServiceException(
                "Failed to parse AI suggestions. The AI model may be overloaded. Please try again.", e
            );
        }
    }
}