package com.example.ai_resume_analyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Externalized configuration for RAG (Retrieval-Augmented Generation) pipeline.
 * All values are configurable via application.properties / environment variables.
 */
@Component
@ConfigurationProperties(prefix = "rag")
public class RagProperties {

    /** Maximum number of chunks to retrieve from vector store per query. */
    private int topK = 5;

    /** Minimum cosine similarity score for a chunk to be considered relevant (0.0 - 1.0). */
    private double similarityThreshold = 0.60;

    /** Maximum number of words per text chunk. */
    private int chunkSize = 500;

    /** Number of words to overlap between consecutive chunks for context continuity. */
    private int chunkOverlap = 50;

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }

    public double getSimilarityThreshold() { return similarityThreshold; }
    public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }

    public int getChunkSize() { return chunkSize; }
    public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }

    public int getChunkOverlap() { return chunkOverlap; }
    public void setChunkOverlap(int chunkOverlap) { this.chunkOverlap = chunkOverlap; }
}
