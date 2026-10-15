package com.rapports.moteur.service.ai;

import java.util.Map;

/**
 * Interface unifiée d'accès aux services LLM (Gemini, Mock, etc.).
 */
public interface AiClient {

    /**
     * Génère du contenu textuel ou JSON via le modèle de langage configuré.
     *
     * @param systemInstruction Directives système structurantes
     * @param userPrompt Prompt utilisateur
     * @param jsonMode Indique si la réponse doit être strictement au format JSON
     * @return Réponse brute du modèle
     */
    String generateContent(String systemInstruction, String userPrompt, boolean jsonMode);

    /**
     * Génère du contenu avec contrôle fin des hyperparamètres et schéma de réponse structuré.
     *
     * @param systemInstruction Directives système structurantes
     * @param userPrompt Prompt utilisateur
     * @param jsonMode Indique si la réponse doit être strictement au format JSON
     * @param temperature Température de décodage (déterminisme)
     * @param responseSchema Schéma OpenAPI 3.0 / Gemini responseSchema optionnel
     * @return Réponse brute du modèle
     */
    default String generateContent(String systemInstruction, String userPrompt, boolean jsonMode, Double temperature, Map<String, Object> responseSchema) {
        return generateContent(systemInstruction, userPrompt, jsonMode);
    }

    /**
     * Indique si le client IA est configuré et disponible pour des requêtes.
     */
    boolean isAvailable();

    /**
     * Nom du fournisseur (ex: "gemini", "mock").
     */
    String getProviderName();
}
