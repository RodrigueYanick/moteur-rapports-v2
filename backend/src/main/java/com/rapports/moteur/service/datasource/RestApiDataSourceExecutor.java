package com.rapports.moteur.service.datasource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.entity.DataSourceAuthType;
import com.rapports.moteur.entity.DataSourceConfig;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.security.UrlSecurityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestApiDataSourceExecutor {

    private final UrlSecurityValidator urlSecurityValidator;
    private final ObjectMapper objectMapper;

    /**
     * Exécute une requête HTTP sur une API REST externe sécurisée.
     */
    public Object executeRest(
            DataSourceConfig config,
            String secretClair,
            String pathOrQuery,
            Map<String, Object> parametres
    ) {
        String fullUrl = resolveFullUrl(config.getUrlOuHote(), pathOrQuery, parametres);
        urlSecurityValidator.validateSafeUrl(fullUrl);

        int timeout = (config.getTimeoutSecondes() != null && config.getTimeoutSecondes() > 0)
                ? config.getTimeoutSecondes() : 10;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .build();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .timeout(Duration.ofSeconds(timeout))
                .header("Accept", "application/json");

        // 1. Authentification
        applyAuthentication(requestBuilder, config, secretClair);

        // 2. En-têtes personnalisés
        applyCustomHeaders(requestBuilder, config.getEnTetesJson());

        // 3. Méthode HTTP
        String method = (config.getMethodeHttp() != null) ? config.getMethodeHttp().toUpperCase(Locale.ROOT) : "GET";
        if ("POST".equals(method)) {
            String jsonBody = (parametres != null && !parametres.isEmpty())
                    ? toJson(parametres) : "{}";
            requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
            requestBuilder.header("Content-Type", "application/json");
        } else {
            requestBuilder.GET();
        }

        try {
            HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                log.warn("L'API distante '{}' a retourné une erreur HTTP {}", fullUrl, response.statusCode());
                throw new ValidationException(String.format("L'API distante a retourné un code d'erreur HTTP %d : %s",
                        response.statusCode(), response.body()));
            }

            String body = response.body();
            if (body == null || body.isBlank()) {
                return Collections.emptyMap();
            }

            JsonNode root = objectMapper.readTree(body);
            if (root.isArray()) {
                return objectMapper.convertValue(root, new TypeReference<List<Map<String, Object>>>() {});
            } else if (root.isObject()) {
                return objectMapper.convertValue(root, new TypeReference<Map<String, Object>>() {});
            } else {
                return Map.of("value", root.asText());
            }

        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            log.error("Échec de l'appel REST vers '{}' : {}", fullUrl, e.getMessage());
            throw new ValidationException("Erreur de communication avec l'API REST distante : " + e.getMessage());
        }
    }

    /**
     * Teste la connectivité avec l'API REST distante.
     */
    public boolean testConnection(DataSourceConfig config, String secretClair, String testPath) {
        String path = (testPath != null && !testPath.isBlank()) ? testPath : "";
        Object result = executeRest(config, secretClair, path, Collections.emptyMap());
        return result != null;
    }

    private String resolveFullUrl(String baseUrl, String pathOrQuery, Map<String, Object> parametres) {
        String base = baseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        String path = (pathOrQuery != null) ? pathOrQuery.trim() : "";
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }

        if (!path.isEmpty() && !path.startsWith("/")) {
            path = "/" + path;
        }

        String full = base + path;

        // Injection des paramètres d'URL si méthode GET
        if (parametres != null && !parametres.isEmpty() && !full.contains("?")) {
            StringJoiner sj = new StringJoiner("&", "?", "");
            parametres.forEach((k, v) -> {
                if (v != null) {
                    sj.add(k + "=" + v);
                }
            });
            full += sj.toString();
        }

        return full;
    }

    private void applyAuthentication(HttpRequest.Builder builder, DataSourceConfig config, String secretClair) {
        DataSourceAuthType authType = config.getAuthType() != null ? config.getAuthType() : DataSourceAuthType.NONE;

        if (secretClair == null || secretClair.isBlank()) {
            return;
        }

        switch (authType) {
            case BEARER -> builder.header("Authorization", "Bearer " + secretClair.trim());
            case BASIC -> {
                String credentials = (config.getNomUtilisateur() != null ? config.getNomUtilisateur() : "") + ":" + secretClair.trim();
                String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
                builder.header("Authorization", "Basic " + encoded);
            }
            case API_KEY -> {
                String headerName = (config.getApiKeyHeader() != null && !config.getApiKeyHeader().isBlank())
                        ? config.getApiKeyHeader().trim() : "X-API-Key";
                builder.header(headerName, secretClair.trim());
            }
            case NONE -> {
                // Aucun en-tête d'authentification requis
            }
        }
    }

    private void applyCustomHeaders(HttpRequest.Builder builder, String enTetesJson) {
        if (enTetesJson == null || enTetesJson.isBlank()) {
            return;
        }

        try {
            Map<String, String> headers = objectMapper.readValue(enTetesJson, new TypeReference<>() {});
            headers.forEach(builder::header);
        } catch (Exception e) {
            log.warn("Impossible de désérialiser les en-têtes JSON personnalisés : {}", e.getMessage());
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
