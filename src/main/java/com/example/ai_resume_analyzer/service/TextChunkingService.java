package com.example.ai_resume_analyzer.service;

import com.example.ai_resume_analyzer.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits resume text into overlapping chunks for embedding generation.
 *
 * Uses a word-boundary sliding window approach with configurable size and overlap.
 * Overlap ensures that important context spanning chunk boundaries is captured
 * by adjacent chunks, improving RAG retrieval quality.
 */
@Service
@RequiredArgsConstructor
public class TextChunkingService {

    private final RagProperties ragProperties;

    /**
     * Splits text into overlapping word-boundary chunks.
     *
     * @param text the normalized resume text
     * @return ordered list of text chunks, empty list if input is null/blank
     */
    public List<String> chunkText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        int chunkSize = ragProperties.getChunkSize();
        int overlap = ragProperties.getChunkOverlap();

        String[] words = text.trim().split("\\s+");
        int totalWords = words.length;

        if (totalWords <= chunkSize) {
            return List.of(text.trim());
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < totalWords) {
            int end = Math.min(start + chunkSize, totalWords);
            StringBuilder chunk = new StringBuilder();
            for (int i = start; i < end; i++) {
                chunk.append(words[i]);
                if (i < end - 1) chunk.append(" ");
            }
            chunks.add(chunk.toString());

            if (end == totalWords) break;
            start = end - overlap;
        }

        return chunks;
    }
}
