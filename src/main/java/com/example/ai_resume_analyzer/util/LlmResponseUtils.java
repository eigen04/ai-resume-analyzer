package com.example.ai_resume_analyzer.util;

/**
 * Shared utilities for cleaning raw LLM responses.
 *
 * LLMs often wrap JSON output in markdown code fences (```json ... ```)
 * even when explicitly instructed not to. This utility strips those wrappers
 * so the response can be parsed by Jackson.
 */
public final class LlmResponseUtils {

    private LlmResponseUtils() {
        // Utility class — not instantiable
    }

    /**
     * Removes markdown code fences (```json ... ```) from LLM output.
     *
     * @param text raw LLM response text
     * @return cleaned text suitable for JSON parsing
     */
    public static String stripCodeFences(String text) {
        return text.replaceAll("(?s)```json\\s*", "")
            .replaceAll("```", "")
            .trim();
    }
}
