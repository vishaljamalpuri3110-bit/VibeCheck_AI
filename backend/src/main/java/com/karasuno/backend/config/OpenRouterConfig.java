package com.karasuno.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for the OpenRouter AI API client.
 * Exposes configuration values and a shared RestTemplate bean.
 */
@Configuration
public class OpenRouterConfig {

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.api.url}")
    private String apiUrl;

    @Value("${openrouter.primary.model}")
    private String primaryModel;

    @Value("${openrouter.fallback.model}")
    private String fallbackModel;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public String getApiKey()       { return apiKey; }
    public String getApiUrl()       { return apiUrl; }
    public String getPrimaryModel() { return primaryModel; }
    public String getFallbackModel(){ return fallbackModel; }
}
