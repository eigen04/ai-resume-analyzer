package com.example.ai_resume_analyzer.service;

import com.example.ai_resume_analyzer.config.RagProperties;
import com.example.ai_resume_analyzer.entity.Resume;
import com.example.ai_resume_analyzer.entity.ResumeStatus;
import com.example.ai_resume_analyzer.exception.DuplicateResumeException;
import com.example.ai_resume_analyzer.exception.InvalidPdfException;
import com.example.ai_resume_analyzer.exception.ResumeNotFoundException;
import com.example.ai_resume_analyzer.repository.ResumeChunkRepository;
import com.example.ai_resume_analyzer.repository.ResumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import com.example.ai_resume_analyzer.repository.UserRepository;
import com.example.ai_resume_analyzer.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ResumeService} with mocked dependencies.
 */
@ExtendWith(MockitoExtension.class)
class ResumeServiceTest {

    @Mock private ResumeRepository resumeRepository;
    @Mock private ResumeChunkRepository resumeChunkRepository;
    @Mock private PdfTextExtractorService pdfTextExtractorService;
    @Mock private TextNormalizationService textNormalizationService;
    @Mock private TextChunkingService textChunkingService;
    @Mock private UserRepository userRepository;
    @Mock private EmbeddingService embeddingService;

    @InjectMocks
    private ResumeService resumeService;

    @BeforeEach
    void setUp() {
        // Setup static SecurityContextHolder for unit tests
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getPrincipal()).thenReturn("testuser");
        SecurityContext context = mock(SecurityContext.class);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        // Standard stubbing for User lookup
        User user = new User();
        user.setUsername("testuser");
        user.setRole("ROLE_USER");
        lenient().when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    }

    @Test
    void shouldThrowInvalidPdfException_whenFileIsEmpty() {
        MockMultipartFile file = new MockMultipartFile("file", new byte[0]);
        file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[0]);

        MockMultipartFile emptyFile = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> resumeService.uploadAndExtract(emptyFile))
            .isInstanceOf(InvalidPdfException.class)
            .hasMessageContaining("empty");
    }

    @Test
    void shouldThrowInvalidPdfException_whenContentTypeIsNotPdf() {
        MockMultipartFile file = new MockMultipartFile(
            "file", "resume.docx", "application/msword", "not a pdf".getBytes()
        );

        assertThatThrownBy(() -> resumeService.uploadAndExtract(file))
            .isInstanceOf(InvalidPdfException.class)
            .hasMessageContaining("PDF");
    }

    @Test
    void shouldThrowDuplicateResumeException_whenSameFileAlreadyExists() throws Exception {
        byte[] pdfBytes = createFakePdfBytes();
        MockMultipartFile file = new MockMultipartFile(
            "file", "resume.pdf", "application/pdf", pdfBytes
        );

        Resume existing = new Resume();
        existing.setId(42L);
        existing.setStatus(ResumeStatus.EMBEDDED);

        when(resumeRepository.findByFileHash(anyString())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> resumeService.uploadAndExtract(file))
            .isInstanceOf(DuplicateResumeException.class)
            .hasMessageContaining("42");
    }

    @Test
    void shouldThrowResumeNotFoundException_whenIdDoesNotExist() {
        when(resumeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resumeService.getById(999L))
            .isInstanceOf(ResumeNotFoundException.class)
            .hasMessageContaining("999");
    }

    // Helper to create minimal bytes that pass the size check (not empty)
    private byte[] createFakePdfBytes() {
        return "%PDF-1.4 fake content for testing".getBytes();
    }
}
