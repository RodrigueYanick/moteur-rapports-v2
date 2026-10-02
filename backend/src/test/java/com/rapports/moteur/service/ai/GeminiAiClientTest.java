package com.rapports.moteur.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.config.AppProperties;
import com.rapports.moteur.exceptions.AiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Tests Unitaires : GeminiAiClient")
class GeminiAiClientTest {

    @Test
    @DisplayName("isAvailable retourne false si la clé API est vide ou par défaut")
    void testIsAvailableWithoutKey() {
        AppProperties props = new AppProperties();
        props.getAi().setApiKey("");
        props.getAi().setEnabled(true);

        GeminiAiClient client = new GeminiAiClient(props, new ObjectMapper());
        assertThat(client.isAvailable()).isFalse();

        props.getAi().setApiKey("placeholder");
        client = new GeminiAiClient(props, new ObjectMapper());
        assertThat(client.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("isAvailable retourne true si la clé API est configurée et activée")
    void testIsAvailableWithKey() {
        AppProperties props = new AppProperties();
        props.getAi().setApiKey("AIzaSyDummyKeyForTest12345");
        props.getAi().setEnabled(true);

        GeminiAiClient client = new GeminiAiClient(props, new ObjectMapper());
        assertThat(client.isAvailable()).isTrue();
        assertThat(client.getProviderName()).isEqualTo("gemini");
    }

    @Test
    @DisplayName("generateContent lève AiException si le client n'est pas disponible")
    void testGenerateContentThrowsWhenUnavailable() {
        AppProperties props = new AppProperties();
        props.getAi().setApiKey("");

        GeminiAiClient client = new GeminiAiClient(props, new ObjectMapper());
        assertThatThrownBy(() -> client.generateContent("sys", "prompt", true))
                .isInstanceOf(AiException.class)
                .hasMessageContaining("n'est pas configuré");
    }
}

