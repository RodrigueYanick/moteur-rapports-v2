package com.rapports.moteur.service;

import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.entity.TemplateStatus;
import com.rapports.moteur.service.expression.SpelSecureExpressionEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemplateHtmlBuilderConditionTest {

    private TemplateHtmlBuilder builder;

    @BeforeEach
    void setUp() {
        CodeGeneratorService codeGen = new CodeGeneratorService();
        SpelSecureExpressionEvaluator evaluator = new SpelSecureExpressionEvaluator();
        builder = new TemplateHtmlBuilder(codeGen, evaluator);
    }

    @Test
    @DisplayName("Masquage conditionnel des blocs si condition = false et affichage si true")
    void testConditionalBlockRendering() {
        String designJson = """
            {
              "pages": [
                {
                  "nom": "Page 1",
                  "blocs": [
                    {
                      "id": "b-remise",
                      "type": "texte",
                      "contenu": "Remise Exceptionnelle Appliquée !",
                      "condition": "remise > 0",
                      "x": 20, "y": 50, "largeurBox": 300, "hauteurBox": 40
                    },
                    {
                      "id": "b-standard",
                      "type": "texte",
                      "contenu": "Facture Standard",
                      "x": 20, "y": 100, "largeurBox": 300, "hauteurBox": 40
                    }
                  ]
                }
              ]
            }
            """;

        ReportTemplate template = ReportTemplate.builder()
                .nom("Test Conditions")
                .contenuDesign(designJson)
                .formatPapier("A4")
                .statut(TemplateStatus.BROUILLON)
                .version(1)
                .build();

        // Cas 1 : remise = 0 -> le bloc remise doit être absent
        Map<String, Object> dataSansRemise = Map.of("remise", 0);
        String htmlSansRemise = builder.build(template, dataSansRemise);

        assertFalse(htmlSansRemise.contains("Remise Exceptionnelle Appliquée !"),
                "Le bloc conditionné remise > 0 ne doit pas être affiché si remise = 0");
        assertTrue(htmlSansRemise.contains("Facture Standard"),
                "Le bloc standard sans condition doit être affiché");

        // Cas 2 : remise = 15 -> le bloc remise doit être présent
        Map<String, Object> dataAvecRemise = Map.of("remise", 15);
        String htmlAvecRemise = builder.build(template, dataAvecRemise);

        assertTrue(htmlAvecRemise.contains("Remise Exceptionnelle Appliquée !"),
                "Le bloc conditionné remise > 0 doit être affiché si remise = 15");
        assertTrue(htmlAvecRemise.contains("Facture Standard"));
    }

    @Test
    @DisplayName("Évaluation d'expressions et fonctions dans le contenu des blocs texte")
    void testExpressionEvaluationInTextBlocks() {
        String designJson = """
            {
              "pages": [
                {
                  "nom": "Page 1",
                  "blocs": [
                    {
                      "id": "b-calc",
                      "type": "texte",
                      "contenu": "Total HT : {{ formatCurrency(qte * prixUnitaire, 'EUR', 'fr-FR') }}",
                      "x": 20, "y": 50, "largeurBox": 400, "hauteurBox": 40
                    }
                  ]
                }
              ]
            }
            """;

        ReportTemplate template = ReportTemplate.builder()
                .nom("Test Calculs")
                .contenuDesign(designJson)
                .formatPapier("A4")
                .statut(TemplateStatus.BROUILLON)
                .version(1)
                .build();

        Map<String, Object> data = Map.of("qte", 4, "prixUnitaire", 250.0);
        String html = builder.build(template, data);

        assertTrue(html.contains("Total HT : 1 000,00 €") || html.contains("Total HT : 1000,00 €"),
                "L'expression qte * prixUnitaire doit être calculée et formatée en devise");
    }

    @Test
    @DisplayName("Tableau dynamique avec colonnes calculées (formule) et agrégats dans tfoot")
    void testDynamicTableComputedColumnsAndAggregates() {
        String designJson = """
            {
              "pages": [
                {
                  "nom": "Page 1",
                  "blocs": [
                    {
                      "id": "b-table",
                      "type": "tableau",
                      "source": "{{articles}}",
                      "x": 20, "y": 50, "largeurBox": 500, "hauteurBox": 150,
                      "colonnes": [
                        { "titre": "Désignation", "variable": "designation" },
                        { "titre": "Quantité", "variable": "qte", "agregat": "SUM" },
                        { "titre": "Total Ligne", "variable": "total", "formule": "qte * pu", "agregat": "SUM" }
                      ]
                    }
                  ]
                }
              ]
            }
            """;

        ReportTemplate template = ReportTemplate.builder()
                .nom("Test Tableau Dynamique")
                .contenuDesign(designJson)
                .formatPapier("A4")
                .statut(TemplateStatus.BROUILLON)
                .version(1)
                .build();

        Map<String, Object> data = Map.of(
                "articles", List.of(
                        Map.of("designation", "Clavier", "qte", 2, "pu", 40.0),
                        Map.of("designation", "Souris", "qte", 3, "pu", 20.0)
                )
        );

        String html = builder.build(template, data);

        // Vérifier les lignes calculées
        assertTrue(html.contains("Clavier"));
        assertTrue(html.contains("80.0") || html.contains("80"));  // 2 * 40
        assertTrue(html.contains("Souris"));
        assertTrue(html.contains("60.0") || html.contains("60"));  // 3 * 20

        // Vérifier la présence du tfoot avec la somme
        assertTrue(html.contains("<tfoot>"), "Un tableau avec agrégat doit comporter un tfoot");
        assertTrue(html.contains("5.00"), "La somme des quantités (2+3) doit être présente dans le pied");
        assertTrue(html.contains("140.00"), "La somme des totaux ligne (80+60) doit être présente dans le pied");
    }

    @Test
    @DisplayName("Tableau dynamique avec ruptures de groupe (Group Headers & Footers) et sous-totaux")
    void testDynamicTableGroupHeadersAndSubtotals() {
        String designJson = """
            {
              "pages": [
                {
                  "nom": "Page 1",
                  "blocs": [
                    {
                      "id": "b-grouped-table",
                      "type": "tableau",
                      "source": "{{articles}}",
                      "groupBy": "categorie",
                      "groupHeaderTemplate": "Famille : {{groupKey}}",
                      "afficherSousTotaux": true,
                      "x": 20, "y": 50, "largeurBox": 500, "hauteurBox": 200,
                      "colonnes": [
                        { "titre": "Article", "variable": "nom" },
                        { "titre": "Quantité", "variable": "qte", "agregat": "SUM" },
                        { "titre": "Montant", "variable": "montant", "agregat": "SUM" }
                      ]
                    }
                  ]
                }
              ]
            }
            """;

        ReportTemplate template = ReportTemplate.builder()
                .nom("Test Ruptures de Groupe")
                .contenuDesign(designJson)
                .formatPapier("A4")
                .statut(TemplateStatus.BROUILLON)
                .version(1)
                .build();

        Map<String, Object> data = Map.of(
                "articles", List.of(
                        Map.of("categorie", "Informatique", "nom", "Écran", "qte", 2, "montant", 300.0),
                        Map.of("categorie", "Informatique", "nom", "Clavier", "qte", 5, "montant", 150.0),
                        Map.of("categorie", "Mobilier", "nom", "Bureau", "qte", 1, "montant", 400.0),
                        Map.of("categorie", "Mobilier", "nom", "Fauteuil", "qte", 2, "montant", 250.0)
                )
        );

        String html = builder.build(template, data);

        // Vérifier les en-têtes de rupture
        assertTrue(html.contains("group-header"), "Le tableau doit comporter des lignes d'en-tête de groupe");
        assertTrue(html.contains("Famille : Informatique"));
        assertTrue(html.contains("Famille : Mobilier"));

        // Vérifier les sous-totaux par rupture
        assertTrue(html.contains("group-footer"), "Le tableau doit comporter des lignes de sous-totaux de groupe");
        assertTrue(html.contains("Sous-total (Informatique)"));
        assertTrue(html.contains("7.00"), "Sous-total quantité Informatique (2+5)");
        assertTrue(html.contains("450.00"), "Sous-total montant Informatique (300+150)");

        assertTrue(html.contains("Sous-total (Mobilier)"));
        assertTrue(html.contains("3.00"), "Sous-total quantité Mobilier (1+2)");
        assertTrue(html.contains("650.00"), "Sous-total montant Mobilier (400+250)");

        // Vérifier le Grand Total dans le tfoot
        assertTrue(html.contains("Total Général"));
        assertTrue(html.contains("10.00"), "Grand total quantité (7+3)");
        assertTrue(html.contains("1100.00"), "Grand total montant (450+650)");
    }

    @Test
    @DisplayName("Génération de graphiques vectoriels SVG haute résolution (bar, line, pie, donut)")
    void testVectorSvgCharts() throws Exception {
        String designJson = """
            {
              "pages": [
                {
                  "nom": "Page 1",
                  "blocs": [
                    {
                      "id": "b-bar",
                      "type": "graphique",
                      "graphiqueType": "bar",
                      "source": "{{stats}}",
                      "graphiqueLabelKey": "mois",
                      "graphiqueValueKey": "ventes",
                      "x": 20, "y": 30, "largeurBox": 300, "hauteurBox": 150
                    },
                    {
                      "id": "b-line",
                      "type": "graphique",
                      "graphiqueType": "line",
                      "source": "{{stats}}",
                      "graphiqueLabelKey": "mois",
                      "graphiqueValueKey": "ventes",
                      "x": 340, "y": 30, "largeurBox": 300, "hauteurBox": 150
                    },
                    {
                      "id": "b-pie",
                      "type": "graphique",
                      "graphiqueType": "pie",
                      "source": "{{repartition}}",
                      "x": 20, "y": 200, "largeurBox": 300, "hauteurBox": 150
                    },
                    {
                      "id": "b-donut",
                      "type": "graphique",
                      "graphiqueType": "donut",
                      "source": "{{repartition}}",
                      "x": 340, "y": 200, "largeurBox": 300, "hauteurBox": 150
                    }
                  ]
                }
              ]
            }
            """;

        ReportTemplate template = ReportTemplate.builder()
                .nom("Test Graphiques SVG")
                .contenuDesign(designJson)
                .formatPapier("A4")
                .statut(TemplateStatus.BROUILLON)
                .version(1)
                .build();

        Map<String, Object> data = Map.of(
                "stats", List.of(
                        Map.of("mois", "Jan", "ventes", 120),
                        Map.of("mois", "Fév", "ventes", 240),
                        Map.of("mois", "Mar", "ventes", 180)
                ),
                "repartition", List.of(
                        Map.of("label", "France", "value", 60),
                        Map.of("label", "Export", "value", 40)
                )
        );

        String html = builder.build(template, data);

        // Vérifier la présence de balises SVG vectorielles
        assertTrue(html.contains("<svg xmlns='http://www.w3.org/2000/svg'"), "Les graphiques doivent générer des balises SVG valides");
        assertTrue(html.contains("<rect x="), "Le graphique à barres doit contenir des éléments rect");
        assertTrue(html.contains("<polyline points="), "Le graphique linéaire doit contenir une polyline");
        assertTrue(html.contains("<path d="), "Le graphique circulaire doit contenir des arcs path");
        assertTrue(html.contains("France (60%)"), "La légende doit afficher les pourcentages calculés");

        // Vérifier que Flying Saucer le compile sans erreur XML
        org.xhtmlrenderer.pdf.ITextRenderer renderer = new org.xhtmlrenderer.pdf.ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        renderer.createPDF(out);
        assertTrue(out.size() > 1000, "Le PDF avec SVG doit être généré sans aucune exception XML");
    }
}

