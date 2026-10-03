package com.rapports.moteur.service.rendering;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlyingSaucerPdfRenderingEngineTest {

    private FlyingSaucerPdfRenderingEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FlyingSaucerPdfRenderingEngine();
    }

    @Test
    void testToValidXhtmlConvertsUnclosedTagsAndAmpersands() {
        String inputHtml = "<html><head><style>p { color: blue; }</style></head>"
                + "<body><h1>Mera & Co.</h1><p>Adresse : Rue 12<br>Douala<hr>Email & contact</p>"
                + "&nbsp;Espace insécable</body></html>";

        String xhtml = engine.toValidXhtml(inputHtml);

        assertNotNull(xhtml);
        assertTrue(xhtml.contains("<br />") || xhtml.contains("<br/>"));
        assertTrue(xhtml.contains("<hr />") || xhtml.contains("<hr/>"));
        assertTrue(xhtml.contains("&amp;"));
        assertFalse(xhtml.contains("&nbsp;")); // Doit être remplacé par &#160;
    }

    @Test
    void testRenderPdfWithUnclosedHtmlTagsSucceedsWithoutException() {
        String rawHtml = "<html><head><style>body { font-family: sans-serif; }</style></head>"
                + "<body>"
                + "<h1>RAPPORT DES VENTES & GESTION</h1>"
                + "<p>Société Mera SARL<br>Douala, Cameroun<br>Tél: +237 600000000</p>"
                + "<hr>"
                + "<p>Facturé à : Client A & Frères</p>"
                + "</body></html>";

        byte[] pdfBytes = engine.render(rawHtml, RenderOptions.builder().build());

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        // Doit commencer par le magic number PDF standard "%PDF-"
        String header = new String(pdfBytes, 0, Math.min(5, pdfBytes.length));
        assertEquals("%PDF-", header);
    }
}

