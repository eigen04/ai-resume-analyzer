package com.example.ai_resume_analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.example.ai_resume_analyzer.config.RagProperties;

/**
 * AI Resume Analyzer — Spring Boot Application Entry Point.
 *
 * Features: PDF text extraction, semantic chunking, vector embeddings,
 * pgvector-based RAG Q&A, job description matching, and improvement suggestions.
 */
@SpringBootApplication
@EnableConfigurationProperties(RagProperties.class)
public class AiResumeAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiResumeAnalyzerApplication.class, args);
    }
}
