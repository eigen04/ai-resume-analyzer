package com.example.ai_resume_analyzer.service;

import com.example.ai_resume_analyzer.dto.interview.InterviewPrepGuide;
import com.example.ai_resume_analyzer.dto.interview.InterviewPrepRequest;
import com.example.ai_resume_analyzer.dto.interview.SerperSearchRequest;
import com.example.ai_resume_analyzer.dto.interview.SerperSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InterviewExperienceService {

    private final ChatClient chatClient;
    private final RestClient restClient;
    private final String serperApiKey;

    private static final String PREP_GUIDE_PROMPT = """
        You are an expert Career Coach and Technical Interview Preparation Guide.
        I will provide you with a Job Description, a Company Name, a Job Title, and raw search snippets containing real historical interview experiences from candidates who applied for this role.

        Your task is to synthesize this information and output a highly structured, chronological interview preparation guide.

        Rules:
        1. Read the raw search snippets carefully. Identify common interview rounds (e.g., Online Assessment, Technical Screen, System Design, Behavioral/HR).
        2. Extract specific questions asked in past interviews for this exact role at this company.
        3. Map the extracted questions to the correct round.
        4. Provide an estimated difficulty for each round (e.g., Easy, Medium, Hard).
        5. Include the source URLs for the extracted questions so the candidate can read the full experience.
        6. **Fallback Rule**: If the search snippets do not contain enough specific data for this exact company/title, generate a highly probable mock interview structure based purely on the Job Description and standard industry practices for that Job Title.

        Job Title: {jobTitle}
        Company Name: {companyName}
        
        Job Description:
        {jobDescription}

        Raw Search Snippets:
        {searchSnippets}
        """;

    public InterviewExperienceService(
            ChatClient.Builder chatClientBuilder,
            RestClient.Builder restClientBuilder,
            @Value("${serper.api.key:dummy_key}") String serperApiKey) {
        this.chatClient = chatClientBuilder.build();
        this.restClient = restClientBuilder.baseUrl("https://google.serper.dev").build();
        this.serperApiKey = serperApiKey;
    }

    public InterviewPrepGuide generatePrepGuide(InterviewPrepRequest request) {
        log.info("Generating interview prep guide for {} at {}", request.getJobTitle(), request.getCompanyName());

        // 1. Construct search query
        String query = String.format("\"%s\" \"%s\" interview experience questions site:leetcode.com OR site:geeksforgeeks.org OR site:reddit.com OR site:glassdoor.com",
                request.getCompanyName(), request.getJobTitle());

        // 2. Execute Web Search via Serper.dev API
        String aggregatedSnippets = "No search results available.";
        try {
            SerperSearchResponse searchResponse = restClient.post()
                    .uri("/search")
                    .header("X-API-KEY", serperApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new SerperSearchRequest(query))
                    .retrieve()
                    .body(SerperSearchResponse.class);

            if (searchResponse != null && searchResponse.getOrganic() != null && !searchResponse.getOrganic().isEmpty()) {
                // Take top 10 results
                List<SerperSearchResponse.OrganicResult> topResults = searchResponse.getOrganic().stream()
                        .limit(10)
                        .toList();

                // Build a combined string of snippets and URLs for the LLM context
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < topResults.size(); i++) {
                    SerperSearchResponse.OrganicResult res = topResults.get(i);
                    sb.append("Result ").append(i + 1).append(":\n");
                    sb.append("Title: ").append(res.getTitle()).append("\n");
                    sb.append("URL: ").append(res.getLink()).append("\n");
                    sb.append("Snippet: ").append(res.getSnippet()).append("\n\n");
                }
                aggregatedSnippets = sb.toString();
            }
        } catch (Exception e) {
            log.warn("Web search failed. Proceeding with fallback mock structure. Error: {}", e.getMessage());
        }

        final String finalSnippets = aggregatedSnippets;

        // 3. Prompt LLM to synthesize the results using structured output
        log.info("Prompting LLM with aggregated search snippets.");
        InterviewPrepGuide guide = chatClient.prompt()
                .user(u -> u.text(PREP_GUIDE_PROMPT)
                        .param("jobTitle", request.getJobTitle())
                        .param("companyName", request.getCompanyName())
                        .param("jobDescription", request.getJobDescription())
                        .param("searchSnippets", finalSnippets))
                .call()
                .entity(InterviewPrepGuide.class);

        if (guide == null) {
            throw new RuntimeException("LLM failed to generate interview prep guide.");
        }

        log.info("Successfully generated structured interview guide with {} rounds.", guide.getRounds().size());
        return guide;
    }
}
