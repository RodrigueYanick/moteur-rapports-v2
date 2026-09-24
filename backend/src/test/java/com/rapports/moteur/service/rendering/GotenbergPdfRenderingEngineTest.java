package com.rapports.moteur.service.rendering;

import com.rapports.moteur.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GotenbergPdfRenderingEngineTest {

    private AppProperties appProperties;
    private GotenbergPdfRenderingEngine gotenbergEngine;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getRendering().setGotenbergEndpoint("http://localhost:3000");
        appProperties.getRendering().setTimeoutSeconds(5);
        gotenbergEngine = new GotenbergPdfRenderingEngine(appProperties);
    }

    @Test
    void testEngineName() {
        assertEquals("gotenberg", gotenbergEngine.getEngineName());
    }

    @Test
    void testIsAvailableReturnsFalseWhenEndpointUnreachable() {
        // Un port où rien ne tourne
        appProperties.getRendering().setGotenbergEndpoint("http://127.0.0.1:54321");
        assertFalse(gotenbergEngine.isAvailable(), "Doit retourner false sans lever d'exception non gérée");
    }

    @Test
    void testRenderThrowsIllegalStateExceptionWhenUnavailable() {
        appProperties.getRendering().setGotenbergEndpoint("http://127.0.0.1:54321");
        RenderOptions options = RenderOptions.defaultA4();
        String simpleHtml = "<html><body><h1>Test Gotenberg</h1></body></html>";

        assertThrows(IllegalStateException.class, () -> {
            gotenbergEngine.render(simpleHtml, options);
        });
    }
}

