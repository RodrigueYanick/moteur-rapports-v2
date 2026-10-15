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
        return generateContent(systemInstruction, userPrompt, jsonMode, null, null);
    }

    @Override
    public String generateContent(String systemInstruction, String userPrompt, boolean jsonMode, Double temperature, Map<String, Object> responseSchema) {
        if (!isAvailable()) {
            throw new AiException("Le fournisseur Gemini n'est pas configuré avec une clé API valide.");
        }

        try {
            return doGenerateContent(systemInstruction, userPrompt, jsonMode, temperature, responseSchema);
        } catch (Exception ex) {
            if (responseSchema != null && !responseSchema.isEmpty()) {
                log.warn("Échec de l'appel Gemini avec responseSchema ({}), tentative de repli en mode JSON standard...", ex.getMessage());
                try {
                    return doGenerateContent(systemInstruction, userPrompt, jsonMode, temperature, null);
                } catch (Exception fallbackEx) {
                    log.error("Échec du repli Gemini : {}", fallbackEx.getMessage(), fallbackEx);
                    throw new AiException("Échec de la communication avec l'API Gemini : " + fallbackEx.getMessage(), fallbackEx);
                }
            }
            log.error("Erreur lors de l'appel à l'API Gemini : {}", ex.getMessage(), ex);
            throw new AiException("Échec de la communication avec l'API Gemini : " + ex.getMessage(), ex);
        }
    }

    private String doGenerateContent(String systemInstruction, String userPrompt, boolean jsonMode, Double temperature, Map<String, Object> responseSchema) {
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

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        if (jsonMode) {
            generationConfig.put("responseMimeType", "application/json");
        }
        if (temperature != null) {
            generationConfig.put("temperature", temperature);
        } else if (aiProperties != null) {
            generationConfig.put("temperature", aiProperties.getTemperatureTemplate());
        }
        if (aiProperties != null) {
            generationConfig.put("topP", aiProperties.getTopP());
            generationConfig.put("topK", aiProperties.getTopK());
            generationConfig.put("maxOutputTokens", aiProperties.getMaxOutputTokens());
        }
        if (responseSchema != null && !responseSchema.isEmpty()) {
            generationConfig.put("responseSchema", responseSchema);
        }

        if (!generationConfig.isEmpty()) {
            requestPayload.put("generationConfig", generationConfig);
        }

        String responseBody = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestPayload)
                .retrieve()
                .body(String.class);

        return extractTextFromGeminiResponse(responseBody);
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
