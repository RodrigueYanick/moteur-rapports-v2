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

    @Test
    void testRenderGraphiqueAndSignaturesWithFlyingSaucer() throws Exception {
        TemplateHtmlBuilder builder = new TemplateHtmlBuilder(new CodeGeneratorService());

        String designJson = """
            {
              "pages": [
                {
                  "nom": "Page 1",
                  "blocs": [
                    {
                      "id": "b-graph",
                      "type": "graphique",
                      "source": "{{statistiques}}",
                      "x": 20,
                      "y": 30,
                      "largeurBox": 320,
                      "hauteurBox": 180,
                      "style": { "fill": "#2563eb" }
                    },
                    {
                      "id": "b-sign",
                      "type": "signature",
                      "x": 20,
                      "y": 230,
                      "largeurBox": 180,
                      "hauteurBox": 70
                    }
                  ]
                }
              ]
            }
            """;

        com.rapports.moteur.entity.ReportTemplate template = com.rapports.moteur.entity.ReportTemplate.builder()
                .nom("Rapport avec Graphique")
                .contenuDesign(designJson)
                .formatPapier("A4")
                .statut(com.rapports.moteur.entity.TemplateStatus.BROUILLON)
                .version(1)
                .build();

        java.util.Map<String, Object> data = java.util.Map.of(
            "statistiques", java.util.List.of(
                java.util.Map.of("label", "Jan", "value", 45),
                java.util.Map.of("label", "Fév", "value", 80),
                java.util.Map.of("label", "Mar", "value", 25),
                java.util.Map.of("label", "Avr", "value", 95)
            )
        );

        String html = builder.build(template, data);

        // Vérifier l'absence totale de flexbox
        org.junit.jupiter.api.Assertions.assertFalse(html.contains("display:flex"));
        org.junit.jupiter.api.Assertions.assertFalse(html.contains("flex:"));
        org.junit.jupiter.api.Assertions.assertFalse(html.contains("align-items:"));

        // Vérifier la présence de la structure de table CSS 2.1
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("table-layout:fixed"));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("vertical-align:bottom"));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("#2563eb")); // couleur personnalisée
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("Jan"));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("Signature"));

        // Vérifier que Flying Saucer le compile en PDF sans aucune erreur
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(out);

        byte[] pdfBytes = out.toByteArray();
        org.junit.jupiter.api.Assertions.assertTrue(pdfBytes.length > 1000, "Le PDF généré par Flying Saucer doit être non vide");
    }
}


