package com.example.ai_resume_analyzer.service;

import com.example.ai_resume_analyzer.config.PromptTemplates;
import com.example.ai_resume_analyzer.dto.JobMatchResponse;
import com.example.ai_resume_analyzer.entity.Resume;
import com.example.ai_resume_analyzer.exception.LlmServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Matches a resume against a job description using a hybrid approach:
 *
 * 1. Keyword Scoring (deterministic):
 *    - Tokenizes both the JD and resume text into lowercase keywords.
 *    - Computes intersection (matched) and difference (missing).
 *    - Calculates a keyword overlap score.
 *
 * 2. Semantic Retrieval:
 *    - Uses vector similarity search to retrieve the most JD-relevant resume chunks.
 *    - Provides the LLM with targeted context rather than flooding it with full text.
 *
 * 3. LLM Analysis (qualitative):
 *    - LLM receives: relevant chunks, JD, pre-computed keyword data.
 *    - LLM generates: final score, strengths, weaknesses, recommendations, summary.
 *    - Score is informed by deterministic keyword data, making it more consistent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeMatchService {

    private final ResumeService resumeService;
    private final EmbeddingService embeddingService;
    private final LlmClientService llmClientService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    // Common stop words to ignore during keyword extraction
    private static final Set<String> STOP_WORDS = Set.of(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
        "of", "with", "by", "from", "is", "are", "was", "were", "be", "been",
        "have", "has", "had", "will", "would", "could", "should", "may", "might",
        "do", "does", "did", "not", "this", "that", "these", "those", "we", "you",
        "they", "it", "its", "our", "your", "their", "as", "if", "than", "then"
    );

    public JobMatchResponse matchAgainstJobDescription(Long resumeId, String jobDescription) {
        Resume resume = resumeService.getById(resumeId);
        String resumeText = resume.getExtractedText();

        if (resumeText == null || resumeText.isBlank()) {
            throw new LlmServiceException("Resume text has not been extracted yet for resumeId: " + resumeId);
        }

        String jdHash = computeMd5(jobDescription);
        String cacheKey = "cache:match:" + resumeId + ":" + jdHash;

        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null && !cachedJson.isBlank()) {
                log.info("Job match cache hit: resumeId={}, hash={}", resumeId, jdHash);
                return objectMapper.readValue(cachedJson, JobMatchResponse.class);
            }
        } catch (Exception e) {
            log.warn("Failed to read from Redis cache for match (continuing without cache): {}", e.getMessage());
        }

        log.info("Starting job match: resumeId={}, hash={}", resumeId, jdHash);

        // Step 1: Deterministic keyword analysis
        Set<String> jdKeywords = extractKeywords(jobDescription);
        Set<String> resumeKeywords = extractKeywords(resumeText);

        List<String> matched = new ArrayList<>(jdKeywords.stream()
            .filter(resumeKeywords::contains)
            .sorted()
            .toList());

        List<String> missing = new ArrayList<>(jdKeywords.stream()
            .filter(k -> !resumeKeywords.contains(k))
            .sorted()
            .toList());

        int keywordScore = jdKeywords.isEmpty() ? 0 :
            (int) Math.round((double) matched.size() / jdKeywords.size() * 100);

        log.debug("Keyword analysis: matched={}, missing={}, keywordScore={}",
            matched.size(), missing.size(), keywordScore);

        // Step 2: Semantic retrieval — get resume chunks most relevant to the JD
        List<Document> relevantChunks = embeddingService.findRelevantChunks(resumeId, jobDescription);
        String resumeContext = relevantChunks.isEmpty()
            ? resumeText.substring(0, Math.min(resumeText.length(), 3000)) // fallback to start of text
            : relevantChunks.stream().map(Document::getContent).collect(Collectors.joining("\n\n---\n\n"));

        // Step 3: LLM qualitative analysis with pre-computed keyword data
        String userPrompt = PromptTemplates.MATCH_USER
            .replace("{resumeContext}", resumeContext)
            .replace("{jobDescription}", jobDescription)
            .replace("{keywordScore}", String.valueOf(keywordScore))
            .replace("{matchedKeywords}", matched.size() > 20 ? matched.subList(0, 20).toString() : matched.toString())
            .replace("{missingKeywords}", missing.size() > 20 ? missing.subList(0, 20).toString() : missing.toString());

        String rawResponse = llmClientService.complete(PromptTemplates.MATCH_SYSTEM, userPrompt);
        String cleaned = stripCodeFences(rawResponse);

        try {
            JobMatchResponse response = objectMapper.readValue(cleaned, JobMatchResponse.class);
            // Ensure matched/missing skills from deterministic analysis are included
            if (response.getMatchedSkills() == null || response.getMatchedSkills().isEmpty()) {
                response.setMatchedSkills(matched.size() > 15 ? matched.subList(0, 15) : matched);
            }
            if (response.getMissingSkills() == null || response.getMissingSkills().isEmpty()) {
                response.setMissingSkills(missing.size() > 10 ? missing.subList(0, 10) : missing);
            }
            log.info("Job match complete: resumeId={}, score={}", resumeId, response.getMatchScore());
            
            try {
                String json = objectMapper.writeValueAsString(response);
                redisTemplate.opsForValue().set(cacheKey, json, java.time.Duration.ofHours(1));
                log.info("Saved job match to cache: resumeId={}, hash={}", resumeId, jdHash);
            } catch (Exception cacheEx) {
                log.warn("Failed to write to Redis cache: {}", cacheEx.getMessage());
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to parse LLM match response for resumeId={}: {}", resumeId, e.getMessage());
            // Return a partial response using deterministic data even if LLM fails
            JobMatchResponse fallback = new JobMatchResponse();
            fallback.setMatchScore(keywordScore);
            fallback.setMatchedSkills(matched.size() > 15 ? matched.subList(0, 15) : matched);
            fallback.setMissingSkills(missing.size() > 10 ? missing.subList(0, 10) : missing);
            fallback.setSummary("Keyword analysis score: " + keywordScore + "%. Full AI analysis temporarily unavailable.");
            return fallback;
        }
    }

    private Set<String> extractKeywords(String text) {
        return Arrays.stream(text.toLowerCase()
            .replaceAll("[^a-z0-9+#.\\s]", " ")
            .split("\\s+"))
            .filter(w -> w.length() > 2)
            .filter(w -> !STOP_WORDS.contains(w))
            .collect(Collectors.toSet());
    }

    private String stripCodeFences(String text) {
        return text.replaceAll("(?s)```json\\s*", "")
            .replaceAll("```", "")
            .trim();
    }

    private String computeMd5(String text) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            return String.valueOf(text.hashCode());
        }
    }
}