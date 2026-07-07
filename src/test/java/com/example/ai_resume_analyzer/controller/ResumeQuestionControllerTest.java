package com.example.ai_resume_analyzer.controller;

import com.example.ai_resume_analyzer.dto.AskResumeRequest;
import com.example.ai_resume_analyzer.dto.AskResumeResponse;
import com.example.ai_resume_analyzer.exception.ResumeNotFoundException;
import com.example.ai_resume_analyzer.handler.GlobalExceptionHandler;
import com.example.ai_resume_analyzer.service.ResumeQuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import com.example.ai_resume_analyzer.repository.UserRepository;
import com.example.ai_resume_analyzer.security.JwtTokenProvider;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-layer tests for {@link ResumeQuestionController} using MockMvc.
 * Tests request validation, correct HTTP status codes, and response format.
 */
@WebMvcTest(ResumeQuestionController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser(username = "testuser", roles = {"USER"})
class ResumeQuestionControllerTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @MockitoBean private ResumeQuestionService resumeQuestionService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;
    @MockitoBean private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        org.springframework.data.redis.core.ValueOperations<String, String> valueOps = org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void shouldReturn200_whenValidQuestionSubmitted() throws Exception {
        AskResumeRequest request = new AskResumeRequest();
        request.setQuestion("What is the candidate's experience with Java?");

        AskResumeResponse response = new AskResumeResponse(
            1L, "The candidate has 3 years of Java experience.", List.of("Java developer with 3 years...")
        );

        when(resumeQuestionService.answerQuestion(eq(1L), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/v1/resumes/1/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.resumeId").value(1))
            .andExpect(jsonPath("$.answer").isNotEmpty())
            .andExpect(jsonPath("$.sources").isArray());
    }

    @Test
    void shouldReturn400_whenQuestionIsBlank() throws Exception {
        AskResumeRequest request = new AskResumeRequest();
        request.setQuestion(""); // blank — should fail @NotBlank

        mockMvc.perform(post("/api/v1/resumes/1/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn404_whenResumeDoesNotExist() throws Exception {
        AskResumeRequest request = new AskResumeRequest();
        request.setQuestion("What is the candidate's Java experience?");

        when(resumeQuestionService.answerQuestion(eq(999L), anyString()))
            .thenThrow(new ResumeNotFoundException(999L));

        mockMvc.perform(post("/api/v1/resumes/999/ask")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("RESUME_NOT_FOUND"))
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturn400_whenRequestBodyIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/resumes/1/ask")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
}
