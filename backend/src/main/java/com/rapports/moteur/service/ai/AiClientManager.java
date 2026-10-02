package com.rapports.moteur.service.ai;

import com.rapports.moteur.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiClientManager {

    private final GeminiAiClient geminiAiClient;
    private final MockAiClient mockAiClient;
    private final AppProperties appProperties;

    /**
     * Retourne le client actif selon la configuration courante.
     */
    public AiClient getActiveClient() {
        AppProperties.Ai ai = appProperties.getAi();
        if (ai != null && ai.isEnabled() && "gemini".equalsIgnoreCase(ai.getProvider()) && geminiAiClient.isAvailable()) {
            return geminiAiClient;
        }
        return mockAiClient;
    }

    /**
     * Exécute une génération avec repli automatique (graceful fallback) sur MockAiClient
     * en cas d'indisponibilité ou d'erreur de l'API distante.
     */
    public String executeWithFallback(String systemInstruction, String userPrompt, boolean jsonMode, AtomicBoolean usedMock) {
        AiClient active = getActiveClient();
        if (active instanceof GeminiAiClient) {
            try {
                String result = active.generateContent(systemInstruction, userPrompt, jsonMode);
                usedMock.set(false);
                return result;
            } catch (Exception ex) {
                log.warn("L'appel à Gemini a échoué ({}), activation du repli déterministe hors-ligne (Mock)...", ex.getMessage());
                usedMock.set(true);
                return mockAiClient.generateContent(systemInstruction, userPrompt, jsonMode);
            }
        } else {
            usedMock.set(true);
            return mockAiClient.generateContent(systemInstruction, userPrompt, jsonMode);
        }
    }

    public boolean isGeminiAvailable() {
        return geminiAiClient.isAvailable();
    }

    public String getCurrentProvider() {
        return getActiveClient().getProviderName();
    }

    public String getModelName() {
        AppProperties.Ai ai = appProperties.getAi();
        return (ai != null && ai.getModel() != null) ? ai.getModel() : "mock-model";
    }

    public boolean isAiEnabled() {
        AppProperties.Ai ai = appProperties.getAi();
        return ai == null || ai.isEnabled();
    }
}
