package com.rapports.moteur.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rapports.moteur.dto.dtoBatch.WebhookTestResponse;
import com.rapports.moteur.security.UrlSecurityValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class WebhookDeliveryService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final UrlSecurityValidator urlSecurityValidator;

    public WebhookDeliveryService(UrlSecurityValidator urlSecurityValidator) {
        this.urlSecurityValidator = urlSecurityValidator;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public WebhookDeliveryService() {
        this(new UrlSecurityValidator());
    }

    /**
     * Envoie un événement webhook avec signature HMAC-SHA256 et politique de retries (jusqu'à 3 tentatives).
     *
     * @param url        URL de destination
     * @param secret     Clé secrète partagée optionnelle
     * @param event      Nom de l'événement (ex: batch.completed)
     * @param payloadObj Objet de données à sérialiser
     * @return true si le webhook a été reçu avec un code HTTP 2xx, false sinon
     */
    public boolean sendWebhook(String url, String secret, String event, Object payloadObj) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            urlSecurityValidator.validateSafeUrl(url);
        } catch (Exception e) {
            log.error("Envoi du webhook annulé pour des raisons de sécurité (SSRF) vers {} : {}", url, e.getMessage());
            return false;
        }

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(payloadObj);
        } catch (Exception e) {
            log.error("Erreur lors de la sérialisation du payload webhook", e);
            return false;
        }

        long timestamp = Instant.now().toEpochMilli();
        String signature = computeHmacSha256(jsonBody, secret);
        String idempotencyKey = computeIdempotencyKey(url, event, jsonBody);

        int maxAttempts = 3;
        int delayMs = 1000;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String deliveryId = UUID.randomUUID().toString();
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(url.trim()))
                        .timeout(Duration.ofSeconds(10))
                        .header("Content-Type", "application/json")
                        .header("User-Agent", "MoteurRapports-Webhook/1.0")
                        .header("X-Webhook-Event", event != null ? event : "batch.event")
                        .header("X-Webhook-Timestamp", String.valueOf(timestamp))
                        .header("Idempotency-Key", idempotencyKey)
                        .header("X-Delivery-ID", deliveryId)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));

                if (signature != null) {
                    reqBuilder.header("X-Webhook-Signature", signature);
                }

                HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();

                if (statusCode >= 200 && statusCode < 300) {
                    log.info("Webhook envoyé avec succès à {} (tentative {}, HTTP {})", url, attempt, statusCode);
                    return true;
                } else if (statusCode >= 500 && attempt < maxAttempts) {
                    log.warn("Webhook reçu une réponse serveur HTTP {} de {}. Nouvelle tentative dans {}ms...", statusCode, url, delayMs);
                    Thread.sleep(delayMs);
                    delayMs *= 2;
                } else {
                    log.warn("Webhook rejeté par {} avec le code HTTP {}", url, statusCode);
                    return false;
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("Envoi du webhook interrompu", ie);
                return false;
            } catch (Exception e) {
                log.warn("Tentative {} échouée pour l'envoi du webhook à {} : {}", attempt, url, e.getMessage());
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(delayMs);
                        delayMs *= 2;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }

        log.error("Échec définitif de l'envoi du webhook vers {} après {} tentatives", url, maxAttempts);
        return false;
    }

    /**
     * Teste la connectivité vers une URL de webhook et mesure la latence.
     */
    public WebhookTestResponse pingWebhook(String url, String secret) {
        if (url == null || url.isBlank()) {
            return WebhookTestResponse.builder()
                    .succes(false)
                    .statusCode(0)
                    .message("URL de webhook non renseignée")
                    .tempsReponseMs(0)
                    .build();
        }

        try {
            urlSecurityValidator.validateSafeUrl(url);
        } catch (Exception e) {
            log.warn("Test de connectivité webhook bloqué (SSRF) vers {} : {}", url, e.getMessage());
            return WebhookTestResponse.builder()
                    .succes(false)
                    .statusCode(400)
                    .message(e.getMessage())
                    .tempsReponseMs(0)
                    .build();
        }

        long start = System.currentTimeMillis();
        Map<String, Object> testPayload = Map.of(
                "event", "webhook.test_ping",
                "timestamp", Instant.now().toString(),
                "message", "Test de connectivité du Moteur de Rapports"
        );

        try {
            String json = objectMapper.writeValueAsString(testPayload);
            long timestamp = Instant.now().toEpochMilli();
            String signature = computeHmacSha256(json, secret);

            String idempotencyKey = UUID.randomUUID().toString();
            String deliveryId = UUID.randomUUID().toString();

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url.trim()))
                    .timeout(Duration.ofSeconds(6))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "MoteurRapports-Webhook/1.0")
                    .header("X-Webhook-Event", "webhook.test_ping")
                    .header("X-Webhook-Timestamp", String.valueOf(timestamp))
                    .header("Idempotency-Key", idempotencyKey)
                    .header("X-Delivery-ID", deliveryId)
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));

            if (signature != null) {
                reqBuilder.header("X-Webhook-Signature", signature);
            }

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            long elapsed = System.currentTimeMillis() - start;
            int status = response.statusCode();

            boolean ok = (status >= 200 && status < 300);
            return WebhookTestResponse.builder()
                    .succes(ok)
                    .statusCode(status)
                    .message(ok ? "Connectivité validée avec succès" : "Réponse HTTP inattendue : " + status)
                    .tempsReponseMs(elapsed)
                    .build();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            return WebhookTestResponse.builder()
                    .succes(false)
                    .statusCode(0)
                    .message("Erreur de connexion : " + e.getMessage())
                    .tempsReponseMs(elapsed)
                    .build();
        }
    }

    /**
     * Calcule la signature HMAC-SHA256 sous la forme sha256=<hex_hash>
     */
    public String computeHmacSha256(String data, String secret) {
        if (secret == null || secret.isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder("sha256=");
            for (byte b : rawHmac) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Erreur lors du calcul HMAC-SHA256", e);
            return null;
        }
    }

    /**
     * Calcule la clé d'idempotence reproductible pour un événement webhook donné.
     */
    public String computeIdempotencyKey(String url, String event, String payload) {
        String safeUrl = url != null ? url.trim() : "";
        String safeEvent = event != null ? event.trim() : "";
        String safePayload = payload != null ? payload : "";
        return UUID.nameUUIDFromBytes((safeUrl + ":" + safeEvent + ":" + safePayload).getBytes(StandardCharsets.UTF_8)).toString();
    }
}
