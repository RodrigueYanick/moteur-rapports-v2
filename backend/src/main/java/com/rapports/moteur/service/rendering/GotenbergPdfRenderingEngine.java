package com.rapports.moteur.service.rendering;

import com.rapports.moteur.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * Moteur de rendu PDF haute-fidélité propulsé par Chromium Headless via le microservice Gotenberg 8.
 * Gère le standard W3C CSS Paged Media (@page), Flexbox, CSS Grid, SVG et les polices Web
 * avec une fidélité d'affichage 100% identique à l'aperçu du navigateur.
 */
@Slf4j
@Component("gotenbergPdfRenderingEngine")
public class GotenbergPdfRenderingEngine implements PdfRenderingEngine {

    public static final String ENGINE_NAME = "gotenberg";

    private final AppProperties appProperties;
    private final HttpClient httpClient;

    @Autowired
    public GotenbergPdfRenderingEngine(AppProperties appProperties) {
        this.appProperties = appProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public GotenbergPdfRenderingEngine(String endpoint, int timeoutSeconds) {
        AppProperties props = new AppProperties();
        props.getRendering().setGotenbergEndpoint(endpoint);
        props.getRendering().setTimeoutSeconds(timeoutSeconds);
        this.appProperties = props;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Override
    public byte[] render(String html, RenderOptions options) {
        String endpoint = getEndpoint();
        String url = endpoint + "/forms/chromium/convert/html";
        int timeoutSeconds = Math.max(5, appProperties.getRendering().getTimeoutSeconds());

        try {
            String boundary = "----GotenbergBoundary" + UUID.randomUUID().toString().replace("-", "");
            byte[] multipartBody = buildMultipartBody(boundary, html, options);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                log.debug("Conversion Gotenberg réussie : {} octets reçus", response.body().length);
                return response.body();
            } else {
                String errorBody = new String(response.body(), StandardCharsets.UTF_8);
                log.error("Échec de la conversion Gotenberg (HTTP {}) : {}", response.statusCode(), errorBody);
                throw new IllegalStateException("Gotenberg conversion failed with HTTP " + response.statusCode() + ": " + errorBody);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interruption lors de l'appel à Gotenberg : " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Erreur de communication avec Gotenberg sur {} : {}", url, e.getMessage());
            throw new IllegalStateException("Erreur Gotenberg : " + e.getMessage(), e);
        }
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    public boolean isAvailable() {
        String healthUrl = getEndpoint() + "/health";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthUrl))
                    .timeout(Duration.ofMillis(1200))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.debug("Gotenberg n'est pas joignable sur {} : {}", healthUrl, e.getMessage());
            return false;
        }
    }

    private String getEndpoint() {
        String endpoint = appProperties.getRendering().getGotenbergEndpoint();
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = "http://localhost:3000";
        }
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint;
    }

    private byte[] buildMultipartBody(String boundary, String html, RenderOptions options) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String lineSeparator = "\r\n";

        // 1. Fichier index.html
        out.write(("--" + boundary + lineSeparator).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"files\"; filename=\"index.html\"" + lineSeparator).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: text/html; charset=utf-8" + lineSeparator + lineSeparator).getBytes(StandardCharsets.UTF_8));
        out.write(html.getBytes(StandardCharsets.UTF_8));
        out.write(lineSeparator.getBytes(StandardCharsets.UTF_8));

        // 2. Paramètre preferCssPageSize (true pour respecter @page CSS de TemplateHtmlBuilder)
        addFormField(out, boundary, "preferCssPageSize", options != null && options.isPreferCssPageSize() ? "true" : "false");

        // 3. Paramètre printBackground (true pour imprimer les couleurs de fond et bordures)
        addFormField(out, boundary, "printBackground", options != null && options.isPrintBackground() ? "true" : "true");

        // 4. Paramètre emulatedMediaType
        String mediaType = (options != null && options.getEmulatedMediaType() != null) ? options.getEmulatedMediaType() : "print";
        addFormField(out, boundary, "emulatedMediaType", mediaType);

        // 5. Marges physiques forcées à 0 (les marges sont gérées par le positionnement CSS @page / millimétrique)
        addFormField(out, boundary, "marginTop", "0");
        addFormField(out, boundary, "marginBottom", "0");
        addFormField(out, boundary, "marginLeft", "0");
        addFormField(out, boundary, "marginRight", "0");

        // Clôture du formulaire multipart
        out.write(("--" + boundary + "--" + lineSeparator).getBytes(StandardCharsets.UTF_8));

        return out.toByteArray();
    }

    private void addFormField(ByteArrayOutputStream out, String boundary, String name, String value) throws IOException {
        String lineSeparator = "\r\n";
        out.write(("--" + boundary + lineSeparator).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"" + lineSeparator + lineSeparator).getBytes(StandardCharsets.UTF_8));
        out.write(value.getBytes(StandardCharsets.UTF_8));
        out.write(lineSeparator.getBytes(StandardCharsets.UTF_8));
    }
}

