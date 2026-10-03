package com.rapports.moteur.service.rendering;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
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
            String xhtml = toValidXhtml(html);
            renderer.setDocumentFromString(xhtml);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Erreur lors du rendu PDF Flying Saucer : {}", e.getMessage(), e);
            throw new IllegalStateException("Erreur lors de la génération du PDF (Flying Saucer) : " + e.getMessage(), e);
        }
    }

    /**
     * Nettoie et transforme le HTML brut en XHTML strict valide requis par Flying Saucer.
     * Corrige automatiquement les balises auto-fermantes (&lt;br&gt;, &lt;hr&gt;, &lt;img&gt;),
     * les entités non déclarées comme &amp;nbsp;, et les esperluettes brutes.
     */
    public String toValidXhtml(String html) {
        if (html == null || html.isBlank()) {
            return "<html><head></head><body></body></html>";
        }
        // Remplacer &nbsp; par &#160; pour satisfaire le parseur XML TrAX
        String preprocessed = html.replace("&nbsp;", "&#160;");

        Document doc = Jsoup.parse(preprocessed, "UTF-8", Parser.htmlParser());
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml);
        doc.outputSettings().escapeMode(org.jsoup.nodes.Entities.EscapeMode.xhtml);
        doc.outputSettings().charset("UTF-8");
        return doc.html();
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
