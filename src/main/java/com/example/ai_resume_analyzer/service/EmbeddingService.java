package com.example.ai_resume_analyzer.service;

import com.example.ai_resume_analyzer.config.RagProperties;
import com.example.ai_resume_analyzer.exception.EmbeddingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Manages embedding generation and vector store operations via Spring AI.
 *
 * Uses pgvector as the backing store. All searches are filtered by resumeId
 * to prevent cross-resume data leakage. Deletion uses native JDBC for reliability
 * since similarity-search-based deletion is unreliable with empty queries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final RagProperties ragProperties;

    /**
     * Generates embeddings for each chunk and stores them in pgvector with resumeId metadata.
     *
     * @param resumeId the ID of the parent resume
     * @param chunkTexts list of text chunks to embed and store
     * @throws EmbeddingException if the vector store add operation fails
     */
    public void storeChunks(Long resumeId, List<String> chunkTexts) {
        log.info("Storing {} embeddings for resumeId={}", chunkTexts.size(), resumeId);
        try {
            List<Document> documents = chunkTexts.stream()
                .map(text -> new Document(
                    text,
                    Map.of("resumeId", String.valueOf(resumeId))
                ))
                .toList();

            vectorStore.add(documents);
            log.info("Successfully stored {} embeddings for resumeId={}", documents.size(), resumeId);
        } catch (Exception e) {
            log.error("Failed to store embeddings for resumeId={}: {}", resumeId, e.getMessage());
            throw new EmbeddingException("Failed to store embeddings for resume " + resumeId, e);
        }
    }

    /**
     * Retrieves the most semantically relevant chunks for a given query,
     * scoped to a specific resume to prevent cross-resume leakage.
     *
     * @param resumeId the resume to search within
     * @param query the natural language query
     * @return ordered list of relevant document chunks, may be empty if nothing meets threshold
     */
    public List<Document> findRelevantChunks(Long resumeId, String query) {
        log.debug("Searching for relevant chunks: resumeId={}, topK={}, threshold={}",
            resumeId, ragProperties.getTopK(), ragProperties.getSimilarityThreshold());

        SearchRequest request = SearchRequest.query(query)
            .withTopK(ragProperties.getTopK())
            .withSimilarityThreshold(ragProperties.getSimilarityThreshold())
            .withFilterExpression("resumeId == '" + resumeId + "'");

        List<Document> results = vectorStore.similaritySearch(request);
        log.debug("Found {} relevant chunks for resumeId={}", results.size(), resumeId);
        return results;
    }

    /**
     * Deletes all vector embeddings associated with a resume using native SQL.
     *
     * Uses JdbcTemplate directly instead of similarity-search workaround,
     * which was unreliable (empty-query cosine similarity is undefined).
     *
     * @param resumeId the ID of the resume whose embeddings should be deleted
     */
    public void deleteByResumeId(Long resumeId) {
        log.info("Deleting embeddings for resumeId={}", resumeId);
        try {
            int deleted = jdbcTemplate.update(
                "DELETE FROM vector_store WHERE metadata->>'resumeId' = ?",
                String.valueOf(resumeId)
            );
            log.info("Deleted {} embedding vectors for resumeId={}", deleted, resumeId);
        } catch (Exception e) {
            log.error("Failed to delete embeddings for resumeId={}: {}", resumeId, e.getMessage());
            throw new EmbeddingException("Failed to delete embeddings for resume " + resumeId, e);
        }
    }
}