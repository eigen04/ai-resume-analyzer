package com.example.ai_resume_analyzer.service;

import com.example.ai_resume_analyzer.dto.ResumeDto;
import com.example.ai_resume_analyzer.entity.Resume;
import com.example.ai_resume_analyzer.entity.ResumeChunk;
import com.example.ai_resume_analyzer.entity.ResumeStatus;
import com.example.ai_resume_analyzer.exception.DuplicateResumeException;
import com.example.ai_resume_analyzer.exception.InvalidPdfException;
import com.example.ai_resume_analyzer.exception.ResumeNotFoundException;
import com.example.ai_resume_analyzer.exception.ResumeProcessingException;
import com.example.ai_resume_analyzer.repository.ResumeChunkRepository;
import com.example.ai_resume_analyzer.repository.ResumeRepository;
import com.example.ai_resume_analyzer.repository.UserRepository;
import com.example.ai_resume_analyzer.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.AccessDeniedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Core service orchestrating the resume upload and processing pipeline.
 *
 * Upload flow:
 *   1. Validate file (type, size, content)
 *   2. Compute SHA-256 hash for duplicate detection
 *   3. Extract text with PDFBox
 *   4. Normalize extracted text
 *   5. Persist resume record with UPLOADED status
 *   6. Chunk text and persist chunks in batch
 *   7. Generate and store embeddings in pgvector
 *   8. Update resume status to EMBEDDED
 *
 * The entire operation runs within a single transaction. On failure,
 * the resume is marked as FAILED and orphan chunks are cleaned up.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeChunkRepository resumeChunkRepository;
    private final UserRepository userRepository;
    private final PdfTextExtractorService pdfTextExtractorService;
    private final TextNormalizationService textNormalizationService;
    private final TextChunkingService textChunkingService;
    private final EmbeddingService embeddingService;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private ResumeService self;

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10MB

    /**
     * Processes an uploaded resume PDF through the full pipeline.
     *
     * @param file the uploaded multipart PDF file
     * @return the persisted and embedded Resume entity
     * @throws InvalidPdfException if the file is not a valid processable PDF
     * @throws DuplicateResumeException if a resume with the same content already exists
     * @throws ResumeProcessingException if any stage of the processing pipeline fails
     */
    public Resume uploadAndExtract(MultipartFile file) throws IOException {
        log.info("Resume upload started: fileName={}, size={} bytes",
            file.getOriginalFilename(), file.getSize());

        // --- Validation ---
        if (file.isEmpty()) {
            throw new InvalidPdfException("Uploaded file is empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidPdfException("Uploaded file exceeds the 10MB size limit.");
        }
        if (!"application/pdf".equals(file.getContentType())) {
            throw new InvalidPdfException("Only PDF files are supported. Received: " + file.getContentType());
        }

        User owner = getAuthenticatedUser();

        // --- Duplicate detection ---
        String fileHash = computeSha256(file.getBytes());
        resumeRepository.findByFileHash(fileHash).ifPresent(existing -> {
            log.warn("Duplicate resume upload detected: existingId={}, fileName={}",
                existing.getId(), existing.getFileName());
            throw new DuplicateResumeException(existing.getId());
        });

        // --- Build resume entity (not yet saved) ---
        Resume resume = new Resume();
        resume.setFileName(file.getOriginalFilename());
        resume.setContentType(file.getContentType());
        resume.setFileHash(fileHash);
        resume.setFileSize(file.getSize());
        resume.setStatus(ResumeStatus.UPLOADED);
        resume.setOwner(owner);

        List<String> chunkTexts;
        try {
            // --- PDF extraction and normalization ---
            String rawText = pdfTextExtractorService.extractText(file);
            String normalizedText = textNormalizationService.normalize(rawText);
            log.info("PDF extracted and normalized: fileName={}, textLength={}",
                file.getOriginalFilename(), normalizedText.length());

            resume.setExtractedText(normalizedText);
            chunkTexts = textChunkingService.chunkText(normalizedText);
            log.info("Text chunked: chunkCount={}", chunkTexts.size());
        } catch (InvalidPdfException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse PDF: fileName={}, error={}", file.getOriginalFilename(), e.getMessage());
            throw new ResumeProcessingException("Failed to extract text from PDF", e);
        }

        // Phase 1: Persist Resume and Chunks (Transactional)
        Resume savedResume = self.persistResumeAndChunks(resume, chunkTexts);

        // Phase 2: Embed (Slow, Network I/O, Non-Transactional)
        try {
            embeddingService.storeChunks(savedResume.getId(), chunkTexts);
            
            // Mark as complete
            savedResume.setStatus(ResumeStatus.EMBEDDED);
            savedResume = resumeRepository.save(savedResume);
            log.info("Resume processing complete: resumeId={}, fileName={}",
                savedResume.getId(), savedResume.getFileName());
            return savedResume;
        } catch (Exception e) {
            log.error("Resume embedding failed: resumeId={}, error={}", savedResume.getId(), e.getMessage());
            savedResume.setStatus(ResumeStatus.FAILED);
            self.cleanupFailedResume(savedResume.getId());
            resumeRepository.save(savedResume);
            throw new ResumeProcessingException("Failed to embed resume chunks", e);
        }
    }

    @Transactional
    public Resume persistResumeAndChunks(Resume resume, List<String> chunkTexts) {
        resume.setStatus(ResumeStatus.TEXT_EXTRACTED);
        Resume saved = resumeRepository.save(resume);
        List<ResumeChunk> chunks = buildChunks(saved, chunkTexts);
        resumeChunkRepository.saveAll(chunks);
        return saved;
    }

    @Transactional
    public void cleanupFailedResume(Long resumeId) {
        resumeChunkRepository.deleteByResumeId(resumeId);
    }

    /**
     * Deletes a resume and all associated data (chunks, embeddings).
     *
     * @param id the resume ID to delete
     * @throws ResumeNotFoundException if no resume exists with the given ID
     */
    @Transactional
    public void deleteResume(Long id) {
        Resume resume = getById(id);
        log.info("Deleting resume: resumeId={}, fileName={}", id, resume.getFileName());

        embeddingService.deleteByResumeId(id);
        resumeChunkRepository.deleteByResumeId(id);
        resumeRepository.delete(resume);

        log.info("Resume deleted successfully: resumeId={}", id);
    }

    /**
     * Retrieves a resume by ID.
     *
     * @throws ResumeNotFoundException if not found
     */
    public Resume getById(Long id) {
        Resume resume = resumeRepository.findById(id)
            .orElseThrow(() -> new ResumeNotFoundException(id));
        
        User currentUser = getAuthenticatedUser();
        if (!resume.getOwner().getUsername().equals(currentUser.getUsername())) {
            log.warn("Access denied to resume: resumeId={}, owner={}, requestedBy={}",
                id, resume.getOwner().getUsername(), currentUser.getUsername());
            throw new AccessDeniedException("You do not own this resume");
        }
        return resume;
    }

    /**
     * Returns a paginated list of resumes owned by the authenticated user.
     */
    public Page<Resume> listAll(Pageable pageable) {
        User user = getAuthenticatedUser();
        return resumeRepository.findByOwnerOrderByCreatedAtDesc(user, pageable);
    }

    /**
     * Returns all text chunks for a given resume.
     */
    public List<ResumeChunk> getChunks(Long resumeId) {
        getById(resumeId); // ensure resume exists
        return resumeChunkRepository.findByResumeIdOrderByChunkIndexAsc(resumeId);
    }

    // --- Private helpers ---

    private List<ResumeChunk> buildChunks(Resume resume, List<String> chunkTexts) {
        return IntStream.range(0, chunkTexts.size())
            .mapToObj(i -> {
                ResumeChunk chunk = new ResumeChunk();
                chunk.setResume(resume);
                chunk.setChunkText(chunkTexts.get(i));
                chunk.setChunkIndex(i);
                return chunk;
            })
            .toList();
    }

    private String computeSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private User getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("User not authenticated or not found in system"));
    }
}