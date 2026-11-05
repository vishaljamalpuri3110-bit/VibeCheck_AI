package com.karasuno.backend.service.compliance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.karasuno.backend.config.OpenRouterConfig;
import com.karasuno.backend.entity.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * HTTP client for the OpenRouter AI API (Module 3 — AI integration).
 *
 * <p>Uses Claude 3.5 Sonnet as the primary model with automatic Llama 3 fallback.
 * Enforces JSON mode in every request so the AI returns strictly structured output
 * matching our compliance schema (verificationItems array).
 */
@Component
public class OpenRouterClient {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterClient.class);

    private final RestTemplate restTemplate;
    private final OpenRouterConfig config;
    private final ObjectMapper objectMapper;

    public OpenRouterClient(RestTemplate restTemplate,
                            OpenRouterConfig config,
                            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    /**
     * Submits the masked draft and reference chunks to OpenRouter for compliance analysis.
     * Attempts the primary model first; automatically falls back to the secondary on any error.
     *
     * @param maskedDraftText  PII-masked marketing draft text
     * @param referenceChunks  document chunks to use as evidence context
     * @return raw JSON string containing the AI's compliance assessment
     */
    public String analyzeCompliance(String maskedDraftText, List<DocumentChunk> referenceChunks) {
        try {
            log.info("Calling primary AI model: {}", config.getPrimaryModel());
            return callOpenRouter(maskedDraftText, referenceChunks, config.getPrimaryModel());
        } catch (Exception primary) {
            log.warn("Primary model failed ({}), falling back to: {}",
                    primary.getMessage(), config.getFallbackModel());
            try {
                return callOpenRouter(maskedDraftText, referenceChunks, config.getFallbackModel());
            } catch (Exception fallback) {
                throw new RuntimeException(
                        "All AI models unavailable: " + fallback.getMessage(), fallback);
            }
        }
    }

    /** Builds and sends a single request to OpenRouter with the specified model. */
    private String callOpenRouter(String maskedDraftText,
                                  List<DocumentChunk> referenceChunks,
                                  String model) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(config.getApiKey());
        // OpenRouter recommended headers for site identification
        headers.set("HTTP-Referer", "https://vibecheckai.karasuno.dev");
        headers.set("X-Title", "VibeCheck AI — CodeNexus India 2026");

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0.1); // Near-zero for deterministic compliance analysis

        ArrayNode messages = objectMapper.createArrayNode();
        messages.addObject()
                .put("role", "system")
                .put("content", buildSystemPrompt(referenceChunks));
        messages.addObject()
                .put("role", "user")
                .put("content", buildUserPrompt(maskedDraftText));
        body.set("messages", messages);

        // Enforce JSON object output mode
        body.set("response_format",
                objectMapper.createObjectNode().put("type", "json_object"));

        HttpEntity<String> entity = new HttpEntity<>(
                objectMapper.writeValueAsString(body), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                config.getApiUrl(), entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("OpenRouter HTTP error: " + response.getStatusCode());
        }

        // Unwrap the content from OpenRouter's response envelope
        JsonNode root = objectMapper.readTree(response.getBody());
        String content = root.path("choices").path(0)
                .path("message").path("content").asText(null);
        if (content == null || content.isBlank()) {
            throw new RuntimeException("OpenRouter returned empty content");
        }
        return content;
    }

    /** Constructs the system prompt embedding all reference chunks as evidence. */
    private String buildSystemPrompt(List<DocumentChunk> referenceChunks) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            You are VibeCheck AI, a precision enterprise compliance verification engine.
            Your task: evaluate each claim in the submitted marketing draft against the reference documents below.

            RULES:
            1. Break the draft into individual claims or sentences.
            2. For each claim assign ONE of these states:
               - SUPPORTED: directly backed by evidence in the reference documents
               - CONTRADICTED: conflicts with or is denied by the reference documents
               - UNVERIFIED: no relevant evidence exists in the reference documents
            3. Assign a confidenceScore between 0.0 and 1.0.
            4. For CONTRADICTED claims, provide a suggestedCorrection that fixes the text surgically.
            5. Cite chunkId, documentName, and pageNumber as evidence.

            RESPONSE — return ONLY valid JSON:
            {
              "verificationItems": [
                {
                  "claim": "string",
                  "state": "SUPPORTED|CONTRADICTED|UNVERIFIED",
                  "confidenceScore": 0.0-1.0,
                  "rationale": "string",
                  "suggestedCorrection": "string or null",
                  "evidenceIds": [
                    { "chunkId": 0, "documentName": "string", "pageNumber": 0, "excerpt": "string" }
                  ]
                }
              ]
            }

            REFERENCE DOCUMENTS:
            """);

        for (DocumentChunk chunk : referenceChunks) {
            sb.append(String.format("[CHUNK_ID:%d | DOC:%s | PAGE:%d]\n%s\n---\n",
                    chunk.getId(), chunk.getDocumentName(),
                    chunk.getPageNumber(), chunk.getContent()));
        }

        return sb.toString();
    }

    /** Constructs the user-turn prompt wrapping the draft text. */
    private String buildUserPrompt(String maskedDraftText) {
        return "Analyze this marketing draft for compliance:\n\n" + maskedDraftText;
    }
}
