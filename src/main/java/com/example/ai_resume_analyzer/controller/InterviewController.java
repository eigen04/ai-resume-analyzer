package com.example.ai_resume_analyzer.controller;

import com.example.ai_resume_analyzer.dto.interview.InterviewPrepGuide;
import com.example.ai_resume_analyzer.dto.interview.InterviewPrepRequest;
import com.example.ai_resume_analyzer.service.InterviewExperienceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
@Tag(name = "Targeted Interview Prep", description = "Endpoints for generating structured, historical interview prep guides")
@SecurityRequirement(name = "Bearer Authentication")
public class InterviewController {

    private final InterviewExperienceService interviewExperienceService;

    @PostMapping("/prep-guide")
    @Operation(summary = "Generate Interview Prep Guide", description = "Performs a web search for real interview experiences at the target company and synthesizes a structured chronological guide via LLM.")
    public ResponseEntity<InterviewPrepGuide> generatePrepGuide(
            @Valid @RequestBody InterviewPrepRequest request) {
        
        InterviewPrepGuide guide = interviewExperienceService.generatePrepGuide(request);
        return ResponseEntity.ok(guide);
    }
}
