package com.rapports.moteur.service;

import com.rapports.moteur.dto.dtoBatch.WebhookTestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebhookDeliveryServiceTest {

    private WebhookDeliveryService webhookService;

    @BeforeEach
    void setUp() {
        webhookService = new WebhookDeliveryService();
    }

    @Test
    @DisplayName("Calcul de la signature HMAC-SHA256 valide")
    void testComputeHmacSha256() {
        String data = "{\"message\":\"hello\"}";
        String secret = "secretKey123";

        String signature = webhookService.computeHmacSha256(data, secret);

        assertThat(signature).isNotNull();
        assertThat(signature).startsWith("sha256=");
        assertThat(signature.length()).isEqualTo(7 + 64); // "sha256=" + 64 hex chars
    }

    @Test
    @DisplayName("Signature nulle si le secret est absent ou vide")
    void testComputeHmacSha256NullSecret() {
        String data = "{\"message\":\"hello\"}";

        assertThat(webhookService.computeHmacSha256(data, null)).isNull();
        assertThat(webhookService.computeHmacSha256(data, "  ")).isNull();
    }

    @Test
    @DisplayName("sendWebhook retourne false immédiatement si l'URL est nulle ou vide")
    void testSendWebhookEmptyUrl() {
        boolean result = webhookService.sendWebhook(null, "secret", "event", "payload");
        assertThat(result).isFalse();

        boolean resultEmpty = webhookService.sendWebhook("   ", "secret", "event", "payload");
        assertThat(resultEmpty).isFalse();
    }

    @Test
    @DisplayName("pingWebhook échoue proprement si l'URL est vide")
    void testPingWebhookEmptyUrl() {
        WebhookTestResponse response = webhookService.pingWebhook("", "secret");

        assertThat(response).isNotNull();
        assertThat(response.isSucces()).isFalse();
        assertThat(response.getMessage()).contains("non renseignée");
    }

    @Test
    @DisplayName("computeIdempotencyKey génère une clé reproductible et déterministe pour un même payload")
    void testComputeIdempotencyKeyDeterministic() {
        String url = "https://erp.example.com/webhook";
        String event = "batch.completed";
        String payload = "{\"batchId\":\"1234\",\"status\":\"TERMINE\"}";

        String key1 = webhookService.computeIdempotencyKey(url, event, payload);
        String key2 = webhookService.computeIdempotencyKey(url, event, payload);

        assertThat(key1).isNotNull();
        assertThat(key1).isEqualTo(key2);

        // Clé différente si le payload change
        String keyDifferentPayload = webhookService.computeIdempotencyKey(url, event, "{\"batchId\":\"9999\"}");
        assertThat(key1).isNotEqualTo(keyDifferentPayload);
    }
}


