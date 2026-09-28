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

    @Test
    void testWatermarkDynamic() throws Exception {
        TemplateHtmlBuilder builder = new TemplateHtmlBuilder(new CodeGeneratorService());

        String designJson = """
            {
              "watermark": {
                "actif": true,
                "texte": "RAPPORT {{statut}}",
                "rotation": -45,
                "opacite": 20,
                "couleur": "#dc2626",
                "fontSize": 50,
                "afficherSur": "TOUTES"
              },
              "pages": [
                {
                  "nom": "Page 1",
                  "blocs": [
                    {
                      "id": "b-texte",
                      "type": "texte",
                      "contenu": "Contenu de la page 1",
                      "x": 10,
                      "y": 10,
                      "largeurBox": 150,
                      "hauteurBox": 30
                    }
                  ]
                }
              ]
            }
            """;

        com.rapports.moteur.entity.ReportTemplate template = com.rapports.moteur.entity.ReportTemplate.builder()
                .nom("Rapport avec Filigrane")
                .contenuDesign(designJson)
                .formatPapier("A4")
                .statut(com.rapports.moteur.entity.TemplateStatus.BROUILLON)
                .version(1)
                .build();

        java.util.Map<String, Object> data = java.util.Map.of("statut", "CONFIDENTIEL");

        String html = builder.build(template, data);

        // 1. Assertions HTML
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("class='watermark'"), "Doit contenir le conteneur du filigrane");
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("RAPPORT CONFIDENTIEL"), "Doit avoir substitué la variable {{statut}}");
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("opacity:0.2"), "L'opacité doit être calculée à 0.2");
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("transform:rotate(-45deg)"), "Doit appliquer la rotation -45deg");
        org.junit.jupiter.api.Assertions.assertTrue(html.contains("color:#dc2626"), "Doit appliquer la couleur configurée");

        // 2. Compilation Flying Saucer PDF
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(out);

        byte[] pdfBytes = out.toByteArray();
        org.junit.jupiter.api.Assertions.assertTrue(pdfBytes.length > 1000, "Le PDF avec filigrane doit être généré correctement");

        // 3. Test avec filigrane inactif
        String inactiveJson = designJson.replace("\"actif\": true", "\"actif\": false");
        com.rapports.moteur.entity.ReportTemplate inactiveTemplate = com.rapports.moteur.entity.ReportTemplate.builder()
                .nom("Rapport sans Filigrane")
                .contenuDesign(inactiveJson)
                .formatPapier("A4")
                .statut(com.rapports.moteur.entity.TemplateStatus.BROUILLON)
                .version(1)
                .build();
        String inactiveHtml = builder.build(inactiveTemplate, data);
        org.junit.jupiter.api.Assertions.assertFalse(inactiveHtml.contains("class='watermark'"),
            "Ne doit pas contenir de filigrane lorsque actif=false");
    }

    @Test
    void testFactureCommercialeDesign() throws Exception {
        TemplateHtmlBuilder builder = new TemplateHtmlBuilder(new CodeGeneratorService());
        String designJson = """
            {"pages": [{"nom": "Page 1", "blocs": [{"x": 38, "y": 38, "type": "rectangle", "style": {"fill": "#2563eb", "couleur": "#94a3b8", "epaisseur": 0, "borderRadius": 0}, "locked": false, "visible": true, "rotation": 0, "hauteurBox": 6, "largeurBox": 718}, {"x": 38, "y": 58, "type": "titre", "style": {"bold": true, "align": "left", "color": "#1e293b", "italic": false, "fontSize": 28, "underline": false, "fontFamily": "Inter", "verticalAlign": "top"}, "locked": false, "contenu": "FACTURE", "visible": true, "rotation": 0, "hauteurBox": 44, "largeurBox": 340}, {"x": 38, "y": 104, "type": "texte", "style": {"bold": false, "align": "left", "color": "#64748b", "italic": false, "fontSize": 11, "underline": false, "fontFamily": "Inter", "verticalAlign": "top"}, "locked": false, "contenu": "N {{numero_facture}}  Date : {{date_facture}}", "visible": true, "rotation": 0, "hauteurBox": 24, "largeurBox": 380}, {"x": 38, "y": 136, "type": "rectangle", "style": {"fill": "#f8fafc", "couleur": "#e2e8f0", "epaisseur": 1, "borderRadius": 6}, "locked": false, "visible": true, "rotation": 0, "hauteurBox": 106, "largeurBox": 340}, {"x": 50, "y": 148, "type": "titre", "style": {"bold": true, "align": "left", "color": "#0f172a", "italic": false, "fontSize": 13, "underline": false, "fontFamily": "Inter", "verticalAlign": "top"}, "locked": false, "contenu": "{{emetteur_nom}}", "visible": true, "rotation": 0, "hauteurBox": 24, "largeurBox": 316}, {"x": 50, "y": 174, "type": "texte", "style": {"bold": false, "align": "left", "color": "#475569", "italic": false, "fontSize": 11, "underline": false, "fontFamily": "Inter", "verticalAlign": "top"}, "locked": false, "contenu": "{{emetteur_adresse}}\\nSIRET : {{emetteur_siret}}", "visible": true, "rotation": 0, "hauteurBox": 58, "largeurBox": 316}, {"x": 416, "y": 136, "type": "rectangle", "style": {"fill": "#f1f5f9", "couleur": "#cbd5e1", "epaisseur": 1, "borderRadius": 6}, "locked": false, "visible": true, "rotation": 0, "hauteurBox": 106, "largeurBox": 340}, {"x": 430, "y": 148, "type": "texte", "style": {"bold": true, "align": "left", "color": "#64748b", "italic": false, "fontSize": 10, "underline": false, "fontFamily": "Inter", "verticalAlign": "top"}, "locked": false, "contenu": "FACTUR? ? :", "visible": true, "rotation": 0, "hauteurBox": 18, "largeurBox": 312}, {"x": 430, "y": 168, "type": "titre", "style": {"bold": true, "align": "left", "color": "#0f172a", "italic": false, "fontSize": 13, "underline": false, "fontFamily": "Inter", "verticalAlign": "top"}, "locked": false, "contenu": "{{client_nom}}", "visible": true, "rotation": 0, "hauteurBox": 24, "largeurBox": 312}, {"x": 430, "y": 192, "type": "texte", "style": {"bold": false, "align": "left", "color": "#475569", "italic": false, "fontSize": 11, "underline": false, "fontFamily": "Inter", "verticalAlign": "top"}, "locked": false, "contenu": "{{client_adresse}}\\n{{client_email}}", "visible": true, "rotation": 0, "hauteurBox": 44, "largeurBox": 312}, {"x": 38, "y": 256, "type": "ligne", "style": {"couleur": "#e2e8f0", "largeur": 718, "epaisseur": 1}, "locked": false, "visible": true, "rotation": 0, "hauteurBox": 4, "largeurBox": 718}, {"x": 38, "y": 272, "type": "tableau", "style": {"bordureCouleur": "#e2e8f0", "texteCouleurDefaut": "#334155"}, "lignes": [[{"value": "DǸsignation / Prestation", "hidden": false, "bgColor": "#f1f5f9", "colSpan": 1, "rowSpan": 1, "textColor": "#1e293b"}, {"value": "QtǸ", "hidden": false, "bgColor": "#f1f5f9", "colSpan": 1, "rowSpan": 1, "textColor": "#1e293b"}, {"value": "Prix Unitaire HT", "hidden": false, "bgColor": "#f1f5f9", "colSpan": 1, "rowSpan": 1, "textColor": "#1e293b"}, {"value": "Total HT", "hidden": false, "bgColor": "#f1f5f9", "colSpan": 1, "rowSpan": 1, "textColor": "#1e293b"}], [{"value": "Prestation de conseil et architecture logicielle", "hidden": false, "colSpan": 1, "rowSpan": 1}, {"value": "5 j", "hidden": false, "colSpan": 1, "rowSpan": 1}, {"value": "850,00 ?", "hidden": false, "colSpan": 1, "rowSpan": 1}, {"value": "4 250,00 ?", "hidden": false, "colSpan": 1, "rowSpan": 1}], [{"value": "DǸveloppement d??interfaces et composants de rapports", "hidden": false, "colSpan": 1, "rowSpan": 1}, {"value": "12 j", "hidden": false, "colSpan": 1, "rowSpan": 1}, {"value": "650,00 ?", "hidden": false, "colSpan": 1, "rowSpan": 1}, {"value": "7 800,00 ?", "hidden": false, "colSpan": 1, "rowSpan": 1}], [{"value": "Formation technique et transfert de compǸtences", "hidden": false, "colSpan": 1, "rowSpan": 1}, {"value": "2 j", "hidden": false, "colSpan": 1, "rowSpan": 1}, {"value": "750,00 ?", "hidden": false, "colSpan": 1, "rowSpan": 1}, {"value": "1 500,00 ?", "hidden": false, "colSpan": 1, "rowSpan": 1}]], "locked": false, "visible": true, "rotation": 0, "hauteurBox": 175, "largeurBox": 718}, {"x": 38, "y": 470, "type": "texte", "style": {"bold": false, "align": "left", "color": "#475569", "italic": false, "fontSize": 11, "underline": false, "fontFamily": "Inter", "verticalAlign": "top"}, "locked": false, "contenu": "Date limite de rglement : {{date_echeance}}\\nMode de rglement : Virement bancaire", "visible": true, "rotation": 0, "hauteurBox": 45, "largeurBox": 380}, {"x": 456, "y": 465, "type": "rectangle", "style": {"fill": "#f8fafc", "couleur": "#e2e8f0", "epaisseur": 1, "borderRadius": 6}, "locked": false, "visible": true, "rotation": 0, "hauteurBox": 120, "largeurBox": 300}, {"x": 470, "y": 476, "type": "texte", "style": {"bold": false, "align": "right", "color": "#334155", "italic": false, "fontSize": 12, "underline": false, "fontFamily": "Inter", "verticalAlign": "top"}, "locked": false, "contenu": "Total HT : {{total_ht}} ?", "visible": true, "rotation": 0, "hauteurBox": 22, "largeurBox": 272}, {"x": 470, "y": 500, "type": "texte", "style": {"bold": false, "align": "right", "color": "#334155", "italic": false, "fontSize": 12, "underline": false, "fontFamily": "Inter", "verticalAlign": "top"}, "locked": false, "contenu": "TVA ({{taux_tva}}%) : {{montant_tva}} ?", "visible": true, "rotation": 0, "hauteurBox": 22, "largeurBox": 272}, {"x": 456, "y": 532, "type": "rectangle", "style": {"fill": "#2563eb", "couleur": "#94a3b8", "epaisseur": 0, "borderRadius": 4}, "locked": false, "visible": true, "rotation": 0, "hauteurBox": 53, "largeurBox": 300}, {"x": 470, "y": 546, "type": "titre", "style": {"bold": true, "align": "right", "color": "#ffffff", "italic": false, "fontSize": 14, "underline": false, "fontFamily": "Inter", "verticalAlign": "top"}, "locked": false, "contenu": "NET ? PAYER : {{total_ttc}} ?", "visible": true, "rotation": 0, "hauteurBox": 26, "largeurBox": 272}, {"x": 38, "y": 980, "type": "texte", "style": {"bold": false, "align": "center", "color": "#94a3b8", "italic": false, "fontSize": 9, "underline": false, "fontFamily": "Inter", "verticalAlign": "top"}, "locked": false, "contenu": "{{conditions_reglement}}\\nPaiement  30 jours date d??Ǹmission. Pas d??escompte pour paiement anticipǸ. En cas de retard de paiement, indemnitǸ forfaitaire pour frais de recouvrement de 40 ?.", "visible": true, "rotation": 0, "hauteurBox": 50, "largeurBox": 718}]}], "watermark": {"type": "TEXTE", "actif": false, "texte": "CONFIDENTIEL", "couleur": "#94a3b8", "opacite": 15, "fontSize": 54, "rotation": -45, "afficherSur": "TOUTES"}}
            """;
        com.rapports.moteur.entity.ReportTemplate template = com.rapports.moteur.entity.ReportTemplate.builder()
                .nom("Facture Commerciale")
                .contenuDesign(designJson)
                .formatPapier("A4")
                .statut(com.rapports.moteur.entity.TemplateStatus.PUBLIE)
                .version(2)
                .build();
        java.util.Map<String, Object> data = java.util.Map.of(
            "date_facture", "2026-09-28",
            "numero_facture", "FAC-001",
            "date_echeance", "2026-10-28",
            "montant_tva", 20.0,
            "total_ht", 100.0,
            "client_adresse", "123 Rue de Paris",
            "client_nom", "Client Test",
            "total_ttc", 120.0
        );
        String html = builder.build(template, data);
        System.out.println("HTML built successfully! Length: " + html.length());

        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        renderer.createPDF(out);
        org.junit.jupiter.api.Assertions.assertNotNull(html);
        org.junit.jupiter.api.Assertions.assertTrue(out.size() > 0, "Le PDF généré ne doit pas être vide");
    }
}




