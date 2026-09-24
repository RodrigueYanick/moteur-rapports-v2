package com.rapports.moteur.service;

import com.rapports.moteur.config.AppProperties;
import com.rapports.moteur.service.rendering.FlyingSaucerPdfRenderingEngine;
import com.rapports.moteur.service.rendering.GotenbergPdfRenderingEngine;
import com.rapports.moteur.service.rendering.PdfRenderingEngine;
import com.rapports.moteur.service.rendering.RenderOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service orchestrateur de rendu PDF avec sélection dynamique de moteur (Feature Toggle)
 * et mécanisme de tolérance aux pannes (Fallback automatique vers Flying Saucer).
 */
@Slf4j
@Service
public class PdfRendererService {

    private final AppProperties appProperties;
    private final Map<String, PdfRenderingEngine> engineMap;
    private final FlyingSaucerPdfRenderingEngine fallbackEngine;

    public PdfRendererService(AppProperties appProperties,
                              List<PdfRenderingEngine> engines,
                              FlyingSaucerPdfRenderingEngine fallbackEngine) {
        this.appProperties = appProperties;
        this.fallbackEngine = fallbackEngine;
        this.engineMap = engines.stream()
                .collect(Collectors.toMap(PdfRenderingEngine::getEngineName, Function.identity(), (a, b) -> a));
    }

    public PdfRendererService() {
        this(new AppProperties(), List.of(new FlyingSaucerPdfRenderingEngine()), new FlyingSaucerPdfRenderingEngine());
    }

    public String getPreferredEngineName() {
        String name = appProperties.getRendering().getEngine();
        return (name != null && !name.isBlank()) ? name : GotenbergPdfRenderingEngine.ENGINE_NAME;
    }

    /**
     * Rendu standard A4 vers flux binaire PDF.
     */
    public byte[] renderToPdf(String html) {
        return renderToPdf(html, RenderOptions.defaultA4());
    }

    /**
     * Rendu haute-fidélité vers flux binaire PDF selon les options de page fournies.
     */
    public byte[] renderToPdf(String html, RenderOptions options) {
        if (options == null) {
            options = RenderOptions.defaultA4();
        }

        String preferredEngineName = appProperties.getRendering().getEngine();
        if (preferredEngineName == null || preferredEngineName.isBlank()) {
            preferredEngineName = GotenbergPdfRenderingEngine.ENGINE_NAME;
        }

        PdfRenderingEngine primary = engineMap.get(preferredEngineName.toLowerCase());

        // Cas 1 : Le moteur préféré est Gotenberg (Chromium Headless)
        if (primary instanceof GotenbergPdfRenderingEngine gotenbergEngine) {
            if (gotenbergEngine.isAvailable()) {
                try {
                    log.debug("Génération PDF via moteur primaire : {}", gotenbergEngine.getEngineName());
                    return gotenbergEngine.render(html, options);
                } catch (Exception e) {
                    log.warn("Échec de la génération avec Gotenberg ({}) : {}. Bascule automatique vers Flying Saucer.",
                            e.getClass().getSimpleName(), e.getMessage());
                }
            } else {
                log.info("Gotenberg n'est pas accessible. Bascule automatique vers le moteur de secours Flying Saucer.");
            }
            return fallbackEngine.render(html, options);
        }

        // Cas 2 : Un autre moteur explicite est configuré (ex: flying-saucer)
        if (primary != null) {
            try {
                return primary.render(html, options);
            } catch (Exception e) {
                log.warn("Erreur sur le moteur {} : {}. Tentative sur moteur de secours.", primary.getEngineName(), e.getMessage());
                return fallbackEngine.render(html, options);
            }
        }

        // Cas 3 : Fallback par défaut
        return fallbackEngine.render(html, options);
    }
}