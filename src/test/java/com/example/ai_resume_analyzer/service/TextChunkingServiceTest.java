package com.example.ai_resume_analyzer.service;

import com.example.ai_resume_analyzer.config.RagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TextChunkingService}.
 *
 * Pure unit tests — no Spring context, no mocks needed.
 * This service is a critical component of the RAG pipeline.
 */
class TextChunkingServiceTest {

    private TextChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        RagProperties props = new RagProperties();
        props.setChunkSize(10); // Small chunk size for predictable test behavior
        props.setChunkOverlap(2);
        chunkingService = new TextChunkingService(props);
    }

    @Test
    void shouldReturnEmptyList_whenInputIsNull() {
        List<String> result = chunkingService.chunkText(null);
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyList_whenInputIsBlank() {
        List<String> result = chunkingService.chunkText("   ");
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnSingleChunk_whenTextIsShorterThanChunkSize() {
        String text = "Java Spring Boot developer with five years experience";
        List<String> result = chunkingService.chunkText(text);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(text.trim());
    }

    @Test
    void shouldReturnSingleChunk_whenWordCountExactlyEqualsChunkSize() {
        // 10 words exactly = chunkSize
        String text = "one two three four five six seven eight nine ten";
        List<String> result = chunkingService.chunkText(text);
        assertThat(result).hasSize(1);
    }

    @Test
    void shouldProduceMultipleChunks_whenTextExceedsChunkSize() {
        // 15 words, chunk=10, overlap=2 → should produce 2 chunks
        String text = "one two three four five six seven eight nine ten eleven twelve thirteen fourteen fifteen";
        List<String> result = chunkingService.chunkText(text);
        assertThat(result).hasSizeGreaterThan(1);
    }

    @Test
    void shouldOverlapBetweenConsecutiveChunks() {
        // 15 words, chunk=10, overlap=2
        // Chunk1: words 0-9, Chunk2: words 8-14 (starts at 10-2=8)
        String text = "w1 w2 w3 w4 w5 w6 w7 w8 w9 w10 w11 w12 w13 w14 w15";
        List<String> result = chunkingService.chunkText(text);

        assertThat(result).hasSizeGreaterThanOrEqualTo(2);
        // The last two words of chunk 1 should appear in the start of chunk 2
        String lastTwoOfChunk1 = "w9 w10";
        assertThat(result.get(1)).startsWith("w9");
    }

    @Test
    void shouldPreserveWordOrder_inEachChunk() {
        String text = "alpha beta gamma delta epsilon zeta eta theta iota kappa lambda mu nu xi omicron";
        List<String> result = chunkingService.chunkText(text);
        assertThat(result.get(0)).startsWith("alpha");
    }

    @Test
    void shouldHandleExcessiveWhitespace_inInput() {
        String text = "word1   word2\t\tword3\n\nword4 word5 word6 word7 word8 word9 word10 word11";
        List<String> result = chunkingService.chunkText(text);
        assertThat(result).isNotEmpty();
        result.forEach(chunk -> assertThat(chunk).doesNotContain("  ")); // no double spaces in output
    }

    @Test
    void shouldReturnChunksWithNoEmptyStrings() {
        String text = "one two three four five six seven eight nine ten eleven twelve thirteen";
        List<String> result = chunkingService.chunkText(text);
        result.forEach(chunk -> assertThat(chunk).isNotBlank());
    }

    @Test
    void shouldCoverAllWords_acrossAllChunks() {
        // The last word of the input must appear in the last chunk
        String text = "a b c d e f g h i j k l m n o p q r s t";
        List<String> result = chunkingService.chunkText(text);
        String lastChunk = result.get(result.size() - 1);
        assertThat(lastChunk).contains("t"); // last word
    }
}
