package com.example.ai_resume_analyzer.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Cleans raw text extracted from PDFs.
 *
 * PDFBox output often contains excessive whitespace, broken newlines,
 * hyphenated words, repeated blank lines, and control characters.
 * This service normalizes the text before it is chunked and embedded.
 */
@Service
public class TextNormalizationService {

    private static final Pattern MULTIPLE_SPACES = Pattern.compile("[ \\t]+");
    private static final Pattern MULTIPLE_NEWLINES = Pattern.compile("(\\r?\\n){3,}");
    private static final Pattern BROKEN_HYPHEN = Pattern.compile("(\\w+)-\\s+(\\w+)");
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]");

    /**
     * Normalizes extracted PDF text for better embedding and RAG quality.
     *
     * @param rawText the raw text from PDFBox
     * @return normalized, clean text, or empty string if input is null/blank
     */
    public String normalize(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }

        String text = rawText;

        // Remove non-printable control characters
        text = CONTROL_CHARS.matcher(text).replaceAll(" ");

        // Rejoin words broken across lines by hyphenation (e.g., "develop-\nment" → "development")
        text = BROKEN_HYPHEN.matcher(text).replaceAll("$1$2");

        // Collapse multiple spaces/tabs into a single space
        text = MULTIPLE_SPACES.matcher(text).replaceAll(" ");

        // Collapse 3+ consecutive blank lines into at most 2
        text = MULTIPLE_NEWLINES.matcher(text).replaceAll("\n\n");

        return text.trim();
    }
}
