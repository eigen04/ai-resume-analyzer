package com.example.ai_resume_analyzer.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TextNormalizationService}.
 * Pure unit tests — no Spring context needed.
 */
class TextNormalizationServiceTest {

    private TextNormalizationService normalizationService;

    @BeforeEach
    void setUp() {
        normalizationService = new TextNormalizationService();
    }

    @Test
    void shouldReturnEmptyString_whenInputIsNull() {
        assertThat(normalizationService.normalize(null)).isEmpty();
    }

    @Test
    void shouldReturnEmptyString_whenInputIsBlank() {
        assertThat(normalizationService.normalize("   \n\t  ")).isEmpty();
    }

    @Test
    void shouldCollapseMultipleSpaces() {
        String input = "Java    Spring     Boot";
        assertThat(normalizationService.normalize(input)).isEqualTo("Java Spring Boot");
    }

    @Test
    void shouldCollapseTabs() {
        String input = "Java\t\t\tSpring\tBoot";
        assertThat(normalizationService.normalize(input)).isEqualTo("Java Spring Boot");
    }

    @Test
    void shouldCollapseExcessiveNewlines() {
        String input = "Section A\n\n\n\n\nSection B";
        String result = normalizationService.normalize(input);
        assertThat(result).doesNotContain("\n\n\n");
        assertThat(result).contains("Section A");
        assertThat(result).contains("Section B");
    }

    @Test
    void shouldRejoinHyphenatedWords() {
        String input = "develop-\nment and imple-\nmentation";
        String result = normalizationService.normalize(input);
        assertThat(result).contains("development");
        assertThat(result).contains("implementation");
    }

    @Test
    void shouldRemoveControlCharacters() {
        String input = "Hello\u0000World\u001FTest";
        String result = normalizationService.normalize(input);
        assertThat(result).doesNotContain("\u0000");
        assertThat(result).doesNotContain("\u001F");
        assertThat(result).contains("Hello");
        assertThat(result).contains("World");
    }

    @Test
    void shouldTrimLeadingAndTrailingWhitespace() {
        String input = "   Java Developer   ";
        assertThat(normalizationService.normalize(input)).isEqualTo("Java Developer");
    }
}
