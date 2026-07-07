package com.example.ai_resume_analyzer.service;

import com.example.ai_resume_analyzer.exception.LlmServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Thin wrapper around Spring AI's {@link ChatModel} for LLM completions.
 *
 * Migrated from a raw RestClient to Spring AI's model abstraction, enabling
 * provider-switching (Ollama → OpenAI → Gemini) via configuration changes only.
 *
 * Provider is configured via:
 *   spring.ai.ollama.chat.options.model=llama3.2:1b
 *   spring.ai.ollama.base-url=http://localhost:11434
 */
@Slf4j
@Service
public class LlmClientService {

    private final ChatModel chatModel;

    public LlmClientService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Sends a system + user prompt to the configured LLM and returns the text response.
     *
     * @param systemPrompt instructions defining LLM behavior
     * @param userPrompt   the actual user message with context
     * @return the LLM text response
     * @throws LlmServiceException if the LLM call fails or returns an empty response
     */
    public String complete(String systemPrompt, String userPrompt) {
        log.debug("Sending LLM request: systemPromptLength={}, userPromptLength={}",
            systemPrompt.length(), userPrompt.length());
        try {
            Prompt prompt = new Prompt(List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
            ));

            String response = chatModel.call(prompt)
                .getResult()
                .getOutput()
                .getContent();

            if (response == null || response.isBlank()) {
                throw new LlmServiceException("LLM returned an empty response.");
            }

            log.debug("LLM response received: length={}", response.length());
            return response;

        } catch (LlmServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("LLM request failed: {}", e.getMessage(), e);
            throw new LlmServiceException("LLM service request failed: " + e.getMessage(), e);
        }
    }
}
