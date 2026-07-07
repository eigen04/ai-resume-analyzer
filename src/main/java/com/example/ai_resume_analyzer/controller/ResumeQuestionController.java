package com.example.ai_resume_analyzer.controller;

import com.example.ai_resume_analyzer.dto.AskResumeRequest;
import com.example.ai_resume_analyzer.dto.AskResumeResponse;
import com.example.ai_resume_analyzer.service.ResumeQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for natural language Q&A about a specific resume using RAG.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
@Tag(name = "Resume Q&A", description = "Ask natural language questions about a resume using RAG")
public class ResumeQuestionController {

    private final ResumeQuestionService resumeQuestionService;

    @Operation(
        summary = "Ask a question about a resume",
        description = "Uses RAG to retrieve relevant resume sections and generate a grounded answer. Returns the answer and supporting source chunks."
    )
    @ApiResponse(responseCode = "200", description = "Question answered successfully")
    @ApiResponse(responseCode = "404", description = "Resume not found")
    @PostMapping("/{resumeId}/ask")
    public ResponseEntity<AskResumeResponse> ask(
            @PathVariable Long resumeId,
            @Valid @RequestBody AskResumeRequest request) {
        AskResumeResponse response = resumeQuestionService.answerQuestion(resumeId, request.getQuestion());
        return ResponseEntity.ok(response);
    }
}