package com.example.ai_resume_analyzer.controller;

import com.example.ai_resume_analyzer.dto.JobMatchRequest;
import com.example.ai_resume_analyzer.dto.JobMatchResponse;
import com.example.ai_resume_analyzer.dto.ResumeSuggestionResponse;
import com.example.ai_resume_analyzer.service.ResumeMatchService;
import com.example.ai_resume_analyzer.service.ResumeSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for AI-powered resume analysis: job matching and improvement suggestions.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
@Tag(name = "Resume Analysis", description = "Job matching and resume improvement endpoints")
public class ResumeMatchController {

    private final ResumeMatchService resumeMatchService;
    private final ResumeSuggestionService resumeSuggestionService;

    @Operation(
        summary = "Match resume against a job description",
        description = "Performs hybrid keyword + semantic analysis and returns a structured match report."
    )
    @ApiResponse(responseCode = "200", description = "Match analysis completed")
    @ApiResponse(responseCode = "404", description = "Resume not found")
    @PostMapping("/{resumeId}/match")
    public ResponseEntity<JobMatchResponse> match(
            @PathVariable Long resumeId,
            @Valid @RequestBody JobMatchRequest request) {
        JobMatchResponse result = resumeMatchService.matchAgainstJobDescription(resumeId, request.getJobDescription());
        return ResponseEntity.ok(result);
    }

    @Operation(
        summary = "Get resume improvement suggestions",
        description = "Generates categorized, specific improvement suggestions. Optionally accepts a job description for targeted advice."
    )
    @ApiResponse(responseCode = "200", description = "Suggestions generated")
    @ApiResponse(responseCode = "404", description = "Resume not found")
    @PostMapping("/{resumeId}/suggestions")
    public ResponseEntity<ResumeSuggestionResponse> suggestions(
            @PathVariable Long resumeId,
            @RequestParam(required = false) String jobDescription) {
        ResumeSuggestionResponse result = resumeSuggestionService.generateSuggestions(resumeId, jobDescription);
        return ResponseEntity.ok(result);
    }
}