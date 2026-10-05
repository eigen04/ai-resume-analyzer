package com.example.ai_resume_analyzer.controller;

import com.example.ai_resume_analyzer.dto.ats.AtsScoreRequest;
import com.example.ai_resume_analyzer.dto.ats.AtsScoreResponse;
import com.example.ai_resume_analyzer.dto.ats.ResumeTailorRequest;
import com.example.ai_resume_analyzer.dto.ats.TailoredResumeResponse;
import com.example.ai_resume_analyzer.service.AtsScoringService;
import com.example.ai_resume_analyzer.service.ResumeGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/resumes/{resumeId}/ats")
@RequiredArgsConstructor
@Tag(name = "ATS & Tailoring", description = "Endpoints for scoring resumes against Job Descriptions and generating tailored LaTeX resumes")
@SecurityRequirement(name = "Bearer Authentication")
public class AtsController {

    private final AtsScoringService atsScoringService;
    private final ResumeGenerationService resumeGenerationService;

    @PostMapping("/score")
    @Operation(summary = "Calculate ATS Score", description = "Evaluates the resume against a provided Job Description using a hybrid keyword/semantic engine.")
    public ResponseEntity<AtsScoreResponse> calculateScore(
            @PathVariable Long resumeId,
            @Valid @RequestBody AtsScoreRequest request) {
        
        AtsScoreResponse response = atsScoringService.calculateScore(resumeId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/tailor")
    @Operation(summary = "Generate Tailored Resume", description = "Rewrites the user's experience to match the Job Description and compiles it into a PDF via a remote LaTeX engine.")
    public ResponseEntity<TailoredResumeResponse> tailorResume(
            @PathVariable Long resumeId,
            @Valid @RequestBody ResumeTailorRequest request) {
        
        TailoredResumeResponse response = resumeGenerationService.generateTailoredResume(resumeId, request);
        return ResponseEntity.ok(response);
    }
}
