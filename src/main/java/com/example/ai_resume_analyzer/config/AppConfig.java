package com.example.ai_resume_analyzer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Application-level bean configuration.
 * Separates infrastructure bean setup from the main application entry point.
 */
@Configuration
public class AppConfig {

    /**
     * Provides a shared RestClient.Builder for dependency injection.
     * Required by Spring AI's OllamaAutoConfiguration.
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
