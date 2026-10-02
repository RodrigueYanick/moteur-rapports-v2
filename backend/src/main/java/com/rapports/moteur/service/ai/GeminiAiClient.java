package com.rapports.moteur.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.config.AppProperties;
import com.rapports.moteur.exceptions.AiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Slf4j
@Component
public class GeminiAiClient implements AiClient {

    private final AppProperties.Ai aiProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiAiClient(AppProperties appProperties, ObjectMapper objectMapper) {
        this.aiProperties = appProperties.getAi();
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMs = (aiProperties != null && aiProperties.getTimeoutSeconds() > 0 ? aiProperties.getTimeoutSeconds() : 25) * 1000;
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public boolean isAvailable() {
        return aiProperties != null 
                && aiProperties.isEnabled() 
                && aiProperties.getApiKey() != null 
                && !aiProperties.getApiKey().isBlank()
                && !"placeholder".equalsIgnoreCase(aiProperties.getApiKey());
    }

    @Override
    public String getProviderName() {
        return "gemini";
    }

    @Override
    public String generateContent(String systemInstruction, String userPrompt, boolean jsonMode) {
        if (!isAvailable()) {
            throw new AiException("Le fournisseur Gemini n'est pas configuré avec une clé API valide.");
        }

        try {
            String url = String.format("%s/models/%s:generateContent?key=%s",
                    aiProperties.getEndpoint(),
                    aiProperties.getModel(),
                    aiProperties.getApiKey());

            Map<String, Object> requestPayload = new LinkedHashMap<>();

            if (systemInstruction != null && !systemInstruction.isBlank()) {
                requestPayload.put("system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemInstruction))
                ));
            }

            requestPayload.put("contents", List.of(
                    Map.of(
                            "role", "user",
                            "parts", List.of(Map.of("text", userPrompt))
                    )
            ));

            if (jsonMode) {
                requestPayload.put("generationConfig", Map.of(
                        "responseMimeType", "application/json"
                ));
            }

            String responseBody = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(String.class);

            return extractTextFromGeminiResponse(responseBody);
        } catch (Exception ex) {
            log.error("Erreur lors de l'appel à l'API Gemini : {}", ex.getMessage(), ex);
            throw new AiException("Échec de la communication avec l'API Gemini : " + ex.getMessage(), ex);
        }
    }

    private String extractTextFromGeminiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText();
                }
            }
            throw new AiException("Réponse inattendue de l'API Gemini : aucun texte extrait");
        } catch (Exception e) {
            if (e instanceof AiException) throw (AiException) e;
            throw new AiException("Impossible d'analyser la réponse Gemini : " + e.getMessage(), e);
        }
    }
}
