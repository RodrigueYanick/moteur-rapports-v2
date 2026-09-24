package com.rapports.moteur.service.rendering;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;

/**
 * Moteur de rendu PDF utilisant Flying Saucer (iText).
 * Fournit une exécution 100% in-memory dans la JVM, servant de moteur de secours
 * résilient si le conteneur Chromium Gotenberg n'est pas actif.
 */
@Slf4j
@Component("flyingSaucerPdfRenderingEngine")
public class FlyingSaucerPdfRenderingEngine implements PdfRenderingEngine {

    public static final String ENGINE_NAME = "flying-saucer";

    @Override
    public byte[] render(String html, RenderOptions options) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors du rendu PDF Flying Saucer : {}", e.getMessage(), e);
            throw new IllegalStateException("Erreur lors de la génération du PDF (Flying Saucer) : " + e.getMessage(), e);
        }
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    public boolean isAvailable() {
        // Toujours disponible car réside nativement dans le classpath de la JVM
        return true;
    }
}

