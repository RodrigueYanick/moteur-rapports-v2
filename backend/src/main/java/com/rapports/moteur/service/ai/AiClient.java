package com.rapports.moteur.service.ai;

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
     * Indique si le client IA est configuré et disponible pour des requêtes.
     */
    boolean isAvailable();

    /**
     * Nom du fournisseur (ex: "gemini", "mock").
     */
    String getProviderName();
}
