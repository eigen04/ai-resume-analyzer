package com.example.ai_resume_analyzer.controller;

import com.example.ai_resume_analyzer.dto.ResumeDto;
import com.example.ai_resume_analyzer.entity.Resume;
import com.example.ai_resume_analyzer.entity.ResumeChunk;
import com.example.ai_resume_analyzer.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * REST controller for core resume management operations.
 * Error handling is delegated to {@link com.example.ai_resume_analyzer.handler.GlobalExceptionHandler}.
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/resumes")
@Tag(name = "Resumes", description = "Resume upload, retrieval, and management endpoints")
public class ResumeController {

    private final ResumeService resumeService;

    @Operation(summary = "Upload a resume PDF", description = "Uploads a PDF resume, extracts text, chunks it, and stores embeddings.")
    @ApiResponse(responseCode = "200", description = "Resume uploaded and processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid or unsupported file")
    @ApiResponse(responseCode = "409", description = "Duplicate resume detected")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadResume(
            @Parameter(description = "PDF resume file (max 10MB)") @RequestParam("file") MultipartFile file)
            throws IOException {
        Resume resume = resumeService.uploadAndExtract(file);
        return ResponseEntity.ok(Map.of(
            "resumeId", resume.getId(),
            "fileName", resume.getFileName(),
            "status", resume.getStatus(),
            "fileSize", resume.getFileSize()
        ));
    }

    @Operation(summary = "List all resumes", description = "Returns a paginated list of all uploaded resumes (most recent first).")
    @GetMapping
    public ResponseEntity<Page<ResumeDto>> listResumes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        Page<ResumeDto> result = resumeService.listAll(pageable).map(r -> new ResumeDto(
            r.getId(), r.getFileName(), r.getContentType(), r.getFileSize(),
            r.getStatus(), r.getCreatedAt(), r.getUpdatedAt()
        ));
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Get resume metadata")
    @GetMapping("/{resumeId}")
    public ResponseEntity<?> getResume(@PathVariable Long resumeId) {
        Resume resume = resumeService.getById(resumeId);
        return ResponseEntity.ok(Map.of(
            "resumeId", resume.getId(),
            "fileName", resume.getFileName(),
            "status", resume.getStatus(),
            "fileSize", resume.getFileSize(),
            "createdAt", resume.getCreatedAt(),
            "updatedAt", resume.getUpdatedAt()
        ));
    }

    @Operation(summary = "Get extracted text", description = "Returns the full extracted text of a resume. Use for debugging only.")
    @GetMapping("/{resumeId}/text")
    public ResponseEntity<?> getResumeText(@PathVariable Long resumeId) {
        Resume resume = resumeService.getById(resumeId);
        return ResponseEntity.ok(Map.of(
            "resumeId", resume.getId(),
            "text", resume.getExtractedText() != null ? resume.getExtractedText() : ""
        ));
    }

    @Operation(summary = "Get resume chunks (debug)", description = "Returns text chunks stored for RAG retrieval. Admin/debug endpoint.")
    @GetMapping("/{resumeId}/chunks")
    public ResponseEntity<?> getChunks(@PathVariable Long resumeId) {
        List<ResumeChunk> chunks = resumeService.getChunks(resumeId);
        return ResponseEntity.ok(chunks.stream().map(c -> Map.of(
            "chunkIndex", c.getChunkIndex(),
            "chunkText", c.getChunkText()
        )).toList());
    }

    @Operation(summary = "Delete a resume", description = "Deletes resume and all associated chunks and embeddings.")
    @ApiResponse(responseCode = "204", description = "Resume deleted")
    @DeleteMapping("/{resumeId}")
    public ResponseEntity<Void> deleteResume(@PathVariable Long resumeId) {
        resumeService.deleteResume(resumeId);
        return ResponseEntity.noContent().build();
    }
}