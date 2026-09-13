package com.rapports.moteur.service;

import org.junit.jupiter.api.Test;
import org.xhtmlrenderer.layout.Layer;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.render.Box;

public class TemplateHtmlBuilderTest {

    @Test
    void testDirectPositioningWithoutPadding() {
        // Test with left: 10mm, top: 15mm directly, padding: 0
        String html = """
            <html><head><style>
            @page { size: 210mm 297mm; margin: 0; }
            html, body { margin: 0; padding: 0; }
            </style></head><body>
            <div class='page' style='position:relative;width:210mm;height:297mm;background:white;'>
              <div style='position:absolute;left:10mm;top:15mm;width:190mm;height:50mm;background:#eee;'>Page 1 Block</div>
            </div>
            <div class='page' style='position:relative;width:210mm;height:297mm;background:white;page-break-before:always;'>
              <div style='position:absolute;left:10mm;top:15mm;width:190mm;height:50mm;background:#eee;'>Page 2 Block</div>
            </div>
            </body></html>
            """;

        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();

        printLayers(renderer.getRootBox().getLayer(), 0);
    }

    private void printLayers(Layer layer, int depth) {
        if (layer == null) return;
        String indent = "  ".repeat(depth);
        Box master = layer.getMaster();
        if (master != null) {
            System.out.println(indent + "Layer master=" + master.getClass().getSimpleName() 
                + " absX=" + master.getAbsX() 
                + " absY=" + master.getAbsY()
                + " (in mm: x=" + String.format("%.2f", master.getAbsX() * 25.4 / 72.0 / 20.0) 
                + " mm, y=" + String.format("%.2f", master.getAbsY() * 25.4 / 72.0 / 20.0) + " mm)");
        }
        for (Layer child : layer.getChildren()) {
            printLayers(child, depth + 1);
        }
    }
}

