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

    @Test
    void testConditionalStylingAndBadges() throws Exception {
        TemplateHtmlBuilder builder = new TemplateHtmlBuilder(new CodeGeneratorService());

        String designJson = """
            {
              "pages": [
                {
                  "nom": "Page 1",
                  "blocs": [
                    {
                      "id": "b-texte-1",
                      "type": "texte",
                      "contenu": "Solde: {{montant_solde}}",
                      "x": 10,
                      "y": 10,
                      "largeurBox": 150,
                      "hauteurBox": 30,
                      "conditionalStyles": [
                        {
                          "id": "r1",
                          "field": "montant_solde",
                          "operator": "GREATER_THAN",
                          "value": "0",
                          "effect": {
                            "color": "#ef4444",
                            "bold": true
                          }
                        }
                      ]
                    },
                    {
                      "id": "b-texte-2",
                      "type": "texte",
                      "contenu": "{{statut}}",
                      "x": 10,
                      "y": 45,
                      "largeurBox": 100,
                      "hauteurBox": 30,
                      "conditionalStyles": [
                        {
                          "id": "r2",
                          "field": "statut",
                          "operator": "EQUALS",
                          "value": "PAYE",
                          "effect": {
                            "badgeStyle": "SUCCESS"
                          }
                        }
                      ]
                    },
                    {
                      "id": "b-table",
                      "type": "tableau",
                      "source": "{{factures}}",
                      "x": 10,
                      "y": 80,
                      "largeurBox": 180,
                      "hauteurBox": 100,
                      "colonnes": [
                        { "variable": "ref", "titre": "Référence" },
                        {
                          "variable": "statut",
                          "titre": "Statut",
                          "conditionalStyles": [
                            {
                              "id": "col-r1",
                              "field": "statut",
                              "operator": "EQUALS",
                              "value": "PAYE",
                              "effect": {
                                "badgeStyle": "SUCCESS"
                              }
                            }
                          ]
                        },
                        { "variable": "montant", "titre": "Montant" }
                      ],
                      "conditionalStyles": [
                        {
                          "id": "row-r1",
                          "field": "montant",
                          "operator": "GREATER_THAN",
                          "value": "100",
                          "effect": {
                            "backgroundColor": "#fef08a"
                          }
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """;

        com.rapports.moteur.entity.ReportTemplate template = com.rapports.moteur.entity.ReportTemplate.builder()
                .nom("Rapport avec Styles Conditionnels")
                .contenuDesign(designJson)
                .formatPapier("A4")
                .statut(com.rapports.moteur.entity.TemplateStatus.BROUILLON)
                .version(1)
                .build();

        java.util.Map<String, Object> data = java.util.Map.of(
            "montant_solde", 150.50,
            "statut", "PAYE",
            "factures", java.util.List.of(
                java.util.Map.of("ref", "FAC-001", "statut", "PAYE", "montant", 150),
                java.util.Map.of("ref", "FAC-002", "statut", "EN_ATTENTE", "montant", 50)
            )
        );

        String html = builder.build(template, data);

        // Vérification du texte conditionnel
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("color:#ef4444"));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("font-weight:bold"));

        // Vérification du badge texte
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("badge-success"));
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("background-color:#dcfce7"));

        // Vérification de la ligne conditionnelle du tableau (montant > 100 => #fef08a)
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("background-color:#fef08a"));

        // Vérification que le badge de colonne de tableau est rendu
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("badge badge-success"));

        // Validation Flying Saucer (rendu PDF)
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(out);

        byte[] pdfBytes = out.toByteArray();
        org.junit.jupiter.api.Assertions.assertTrue(pdfBytes.length > 1000, "Le PDF généré avec styles conditionnels doit être valide");
    }

    @Test
    void testTablePageBreaksAndHeaderRepeat() throws Exception {
        TemplateHtmlBuilder builder = new TemplateHtmlBuilder(new CodeGeneratorService());

        String designJson = """
            {
              "pages": [
                {
                  "nom": "Page 1",
                  "blocs": [
                    {
                      "id": "b-table-dynamic",
                      "type": "tableau",
                      "source": "{{factures}}",
                      "x": 10,
                      "y": 10,
                      "largeurBox": 180,
                      "hauteurBox": 100,
                      "repeterEnTeteChaquePage": true,
                      "eviterCoupureLignes": true,
                      "colonnes": [
                        { "variable": "ref", "titre": "Référence" },
                        { "variable": "montant", "titre": "Montant" }
                      ]
                    },
                    {
                      "id": "b-table-static",
                      "type": "tableau",
                      "x": 10,
                      "y": 120,
                      "largeurBox": 180,
                      "hauteurBox": 60,
                      "repeterEnTeteChaquePage": true,
                      "eviterCoupureLignes": true,
                      "lignes": [
                        [ { "value": "Article" }, { "value": "Prix" } ],
                        [ { "value": "Abonnement" }, { "value": "50€" } ]
                      ]
                    }
                  ]
                }
              ]
            }
            """;

        com.rapports.moteur.entity.ReportTemplate template = com.rapports.moteur.entity.ReportTemplate.builder()
                .nom("Rapport avec Ruptures de Page")
                .contenuDesign(designJson)
                .formatPapier("A4")
                .statut(com.rapports.moteur.entity.TemplateStatus.BROUILLON)
                .version(1)
                .build();

        java.util.Map<String, Object> data = java.util.Map.of(
            "factures", java.util.List.of(
                java.util.Map.of("ref", "FAC-001", "montant", 150),
                java.util.Map.of("ref", "FAC-002", "montant", 250)
            )
        );

        String html = builder.build(template, data);

        // 1. Structure sémantique thead / tbody
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("<thead>"), "Doit contenir la balise thead pour la répétition");
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("<tbody>"), "Doit contenir la balise tbody");
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("<th style="), "Les en-têtes doivent utiliser th");

        // 2. Éviter coupure des lignes
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("page-break-inside:avoid;break-inside:avoid;"),
            "Les lignes doivent avoir les styles d'évitement de coupure");

        // 3. Validation de non-régression Flying Saucer PDF
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(out);

        byte[] pdfBytes = out.toByteArray();
        org.junit.jupiter.api.Assertions.assertTrue(pdfBytes.length > 1000, "Le PDF multi-page généré par Flying Saucer doit être valide");

        // 4. Test avec options désactivées
        String disabledDesign = designJson
            .replace("\"repeterEnTeteChaquePage\": true", "\"repeterEnTeteChaquePage\": false")
            .replace("\"eviterCoupureLignes\": true", "\"eviterCoupureLignes\": false");

        com.rapports.moteur.entity.ReportTemplate disabledTemplate = com.rapports.moteur.entity.ReportTemplate.builder()
                .nom("Rapport sans Ruptures")
                .contenuDesign(disabledDesign)
                .formatPapier("A4")
                .statut(com.rapports.moteur.entity.TemplateStatus.BROUILLON)
                .version(1)
                .build();

        String disabledHtml = builder.build(disabledTemplate, data);
        org.junit.jupiter.api.Assertions.assertFalse(disabledHtml.contains("page-break-inside:avoid;break-inside:avoid;"),
            "Ne doit pas contenir page-break-inside si désactivé");
    }
}



