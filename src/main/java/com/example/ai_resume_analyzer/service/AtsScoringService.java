package com.example.ai_resume_analyzer.service;

import com.example.ai_resume_analyzer.dto.ats.AtsScoreRequest;
import com.example.ai_resume_analyzer.dto.ats.AtsScoreResponse;
import com.example.ai_resume_analyzer.dto.ats.KeywordExtractionResult;
import com.example.ai_resume_analyzer.dto.ats.SemanticEvaluationResult;
import com.example.ai_resume_analyzer.entity.Resume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AtsScoringService {

    private final ChatClient chatClient;
    private final ResumeService resumeService;

    private static final String KEYWORD_PROMPT = """
        You are an expert technical recruiter and ATS system.
        Analyze the following Job Description (Role: {role}, Company: {company}, Experience required: {experience}).
        Extract exactly the top 20 mandatory technical and soft skill keywords required for this job.
        
        Job Description:
        {jobDescription}
        """;

    private static final String SEMANTIC_PROMPT = """
        You are an expert technical recruiter and ATS system.
        Evaluate the following resume text against the Job Description.
        
        Provide a semantic matching score from 0 to 100 based on how well the candidate's experience, depth of knowledge, and context align with the job requirements.
        Also provide a short critique (max 3 sentences) on the match quality.
        
        Job Description:
        {jobDescription}
        
        Resume Text:
        {resumeText}
        """;

    public AtsScoringService(ChatClient.Builder chatClientBuilder, ResumeService resumeService) {
        this.chatClient = chatClientBuilder.build();
        this.resumeService = resumeService;
    }

    public AtsScoreResponse calculateScore(Long resumeId, AtsScoreRequest request) {
        Resume resume = resumeService.getById(resumeId);
        String resumeText = resume.getExtractedText();

        if (resumeText == null || resumeText.isBlank()) {
            throw new IllegalArgumentException("Resume text is empty. Cannot evaluate ATS score.");
        }

        // Step A: Extract Keywords
        KeywordExtractionResult keywordResult = chatClient.prompt()
            .user(u -> u.text(KEYWORD_PROMPT)
                .param("role", request.getRole())
                .param("company", request.getCompanyName() != null ? request.getCompanyName() : "Unknown")
                .param("experience", request.getYearsOfExperience() != null ? request.getYearsOfExperience() : "Not specified")
                .param("jobDescription", request.getJobDescription()))
            .call()
            .entity(KeywordExtractionResult.class);

        List<String> keywords = keywordResult != null && keywordResult.getKeywords() != null 
            ? keywordResult.getKeywords() : new ArrayList<>();

        // Step B: Deterministic Keyword Matching
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        
        String lowerResume = resumeText.toLowerCase();

        for (String kw : keywords) {
            String lowerKw = kw.toLowerCase().trim();
            // Regex to find keyword as a whole word or bounded by punctuation
            String regex = "(?i)\\b" + Pattern.quote(lowerKw) + "\\b";
            if (Pattern.compile(regex).matcher(lowerResume).find() || lowerResume.contains(lowerKw)) {
                matched.add(kw);
            } else {
                missing.add(kw);
            }
        }

        // Calculate keyword match score out of 40
        double keywordScore = 0.0;
        if (!keywords.isEmpty()) {
            keywordScore = ((double) matched.size() / keywords.size()) * 40.0;
        }

        // Step C: Semantic LLM Evaluation
        SemanticEvaluationResult semanticResult = chatClient.prompt()
            .user(u -> u.text(SEMANTIC_PROMPT)
                .param("jobDescription", request.getJobDescription())
                .param("resumeText", resumeText))
            .call()
            .entity(SemanticEvaluationResult.class);

        int rawSemanticScore = semanticResult != null ? semanticResult.getSemanticScore() : 0;
        String critique = semanticResult != null ? semanticResult.getCritique() : "Evaluation failed.";

        // Calculate semantic score out of 60
        double semanticScore = (rawSemanticScore / 100.0) * 60.0;

        // Final Score
        int finalScore = (int) Math.round(keywordScore + semanticScore);

        return AtsScoreResponse.builder()
            .finalScore(finalScore)
            .keywordMatchScore(Math.round(keywordScore * 10.0) / 10.0)
            .semanticScore(Math.round(semanticScore * 10.0) / 10.0)
            .matchedKeywords(matched)
            .missingKeywords(missing)
            .llmCritique(critique)
            .build();
    }
}
