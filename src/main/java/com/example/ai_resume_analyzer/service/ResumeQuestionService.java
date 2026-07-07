package com.example.ai_resume_analyzer.service;

import com.example.ai_resume_analyzer.config.PromptTemplates;
import com.example.ai_resume_analyzer.dto.AskResumeResponse;
import com.example.ai_resume_analyzer.exception.LlmServiceException;
import com.example.ai_resume_analyzer.exception.ResumeNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeQuestionService {

    private final ResumeService resumeService;
    private final EmbeddingService embeddingService;
    private final LlmClientService llmClientService;

    /**
     * Answers a natural language question about a specific resume.
     *
     * @param resumeId the resume to query
     * @param question the natural language question
     * @return the answer with supporting source chunks
     * @throws ResumeNotFoundException if the resume does not exist
     * @throws LlmServiceException if no relevant context is found or LLM fails
     */
    public AskResumeResponse answerQuestion(Long resumeId, String question) {
        resumeService.getById(resumeId); // 404 guard

        log.info("Processing Q&A: resumeId={}, questionLength={}", resumeId, question.length());

        List<Document> relevantChunks = embeddingService.findRelevantChunks(resumeId, question);

        if (relevantChunks.isEmpty()) {
            log.warn("No relevant chunks found for Q&A: resumeId={}", resumeId);
            return new AskResumeResponse(
                resumeId,
                "The resume does not contain enough relevant information to answer this question.",
                List.of()
            );
        }

        List<String> sourceTexts = relevantChunks.stream()
            .map(Document::getContent)
            .toList();

        String context = String.join("\n\n---\n\n", sourceTexts);

        String userPrompt = PromptTemplates.QA_USER
            .replace("{context}", context)
            .replace("{question}", question);

        log.debug("Q&A LLM call: resumeId={}, chunks={}", resumeId, relevantChunks.size());
        String answer = llmClientService.complete(PromptTemplates.QA_SYSTEM, userPrompt);

        log.info("Q&A complete: resumeId={}, answerLength={}", resumeId, answer.length());
        return new AskResumeResponse(resumeId, answer, sourceTexts);
    }
}