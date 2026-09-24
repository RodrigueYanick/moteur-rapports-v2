package com.rapports.moteur.service;

import com.rapports.moteur.config.AppProperties;
import com.rapports.moteur.service.rendering.FlyingSaucerPdfRenderingEngine;
import com.rapports.moteur.service.rendering.GotenbergPdfRenderingEngine;
import com.rapports.moteur.service.rendering.RenderOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfRendererServiceTest {

    @Mock
    private GotenbergPdfRenderingEngine gotenbergEngine;

    @Mock
    private FlyingSaucerPdfRenderingEngine fallbackEngine;

    private AppProperties appProperties;
    private PdfRendererService pdfRendererService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        when(gotenbergEngine.getEngineName()).thenReturn("gotenberg");
        when(fallbackEngine.getEngineName()).thenReturn("flying-saucer");

        pdfRendererService = new PdfRendererService(
                appProperties,
                List.of(gotenbergEngine, fallbackEngine),
                fallbackEngine
        );
    }

    @Test
    void testRenderWithGotenbergWhenAvailable() {
        appProperties.getRendering().setEngine("gotenberg");
        when(gotenbergEngine.isAvailable()).thenReturn(true);
        byte[] expectedPdf = new byte[]{1, 2, 3};
        when(gotenbergEngine.render(any(), any())).thenReturn(expectedPdf);

        byte[] result = pdfRendererService.renderToPdf("<html><body>Hello</body></html>");

        assertArrayEquals(expectedPdf, result);
        verify(gotenbergEngine, times(1)).render(any(), any());
        verify(fallbackEngine, never()).render(any(), any());
    }

    @Test
    void testFallbackToFlyingSaucerWhenGotenbergUnavailable() {
        appProperties.getRendering().setEngine("gotenberg");
        when(gotenbergEngine.isAvailable()).thenReturn(false);
        byte[] expectedFallbackPdf = new byte[]{4, 5, 6};
        when(fallbackEngine.render(any(), any())).thenReturn(expectedFallbackPdf);

        byte[] result = pdfRendererService.renderToPdf("<html><body>Hello</body></html>");

        assertArrayEquals(expectedFallbackPdf, result);
        verify(gotenbergEngine, never()).render(any(), any());
        verify(fallbackEngine, times(1)).render(any(), any());
    }

    @Test
    void testFallbackToFlyingSaucerWhenGotenbergThrowsException() {
        appProperties.getRendering().setEngine("gotenberg");
        when(gotenbergEngine.isAvailable()).thenReturn(true);
        when(gotenbergEngine.render(any(), any())).thenThrow(new IllegalStateException("Gotenberg HTTP 500"));
        byte[] expectedFallbackPdf = new byte[]{7, 8, 9};
        when(fallbackEngine.render(any(), any())).thenReturn(expectedFallbackPdf);

        byte[] result = pdfRendererService.renderToPdf("<html><body>Hello</body></html>");

        assertArrayEquals(expectedFallbackPdf, result);
        verify(gotenbergEngine, times(1)).render(any(), any());
        verify(fallbackEngine, times(1)).render(any(), any());
    }

    @Test
    void testExplicitFlyingSaucerSelection() {
        appProperties.getRendering().setEngine("flying-saucer");
        byte[] expectedPdf = new byte[]{9, 9, 9};
        when(fallbackEngine.render(any(), any())).thenReturn(expectedPdf);

        byte[] result = pdfRendererService.renderToPdf("<html><body>Hello</body></html>");

        assertArrayEquals(expectedPdf, result);
        verify(fallbackEngine, times(1)).render(any(), any());
    }
}

