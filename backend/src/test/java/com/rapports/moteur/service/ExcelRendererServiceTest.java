package com.rapports.moteur.service;

import com.rapports.moteur.entity.Categorie;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.entity.TemplateStatus;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelRendererServiceTest {

    private ExcelRendererService excelRendererService;

    @BeforeEach
    void setUp() {
        excelRendererService = new ExcelRendererService();
    }

    @Test
    @DisplayName("Doit générer un classeur Excel complet avec synthèse et feuilles de tableaux")
    void shouldGenerateFullExcelWorkbookWithSummaryAndTableSheets() throws IOException {
        // Given
        ReportTemplate template = ReportTemplate.builder()
                .id(UUID.randomUUID())
                .nom("Facture Vente")
                .statut(TemplateStatus.PUBLIE)
                .categorie(Categorie.FINANCE)
                .codeEntreprise("ENT-001")
                .description("Modèle de facture standard")
                .contenuDesign("""
                    {
                      "pages": [
                        {
                          "id": "page-1",
                          "nom": "Page 1",
                          "blocs": [
                            {
                              "id": "b-table",
                              "type": "tableau",
                              "nom": "Lignes de Commande",
                              "source": "{{lignes}}",
                              "colonnes": [
                                { "titre": "Désignation", "variable": "designation" },
                                { "titre": "Quantité", "variable": "quantite" },
                                { "titre": "Prix Unitaire", "variable": "prix_unitaire" }
                              ]
                            }
                          ]
                        }
                      ]
                    }
                    """)
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("client_nom", "Acme Corporation");
        data.put("numero_facture", "FAC-2026-001");
        data.put("est_valide", true);

        List<Map<String, Object>> lignes = new ArrayList<>();
        Map<String, Object> l1 = new HashMap<>();
        l1.put("designation", "Licence Pro");
        l1.put("quantite", 5);
        l1.put("prix_unitaire", 120.50);
        lignes.add(l1);

        Map<String, Object> l2 = new HashMap<>();
        l2.put("designation", "Support Dédié");
        l2.put("quantite", 1);
        l2.put("prix_unitaire", 450.00);
        lignes.add(l2);

        data.put("lignes", lignes);

        // When
        byte[] excelBytes = excelRendererService.renderToExcel(template, data);

        // Then
        assertThat(excelBytes).isNotNull().isNotEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            // Vérification de la feuille Synthèse
            XSSFSheet summarySheet = workbook.getSheet("Synthèse");
            assertThat(summarySheet).isNotNull();

            Row titleRow = summarySheet.getRow(0);
            assertThat(titleRow.getCell(0).getStringCellValue()).isEqualTo("Facture Vente");

            // Vérification de la présence des variables scalaires
            boolean foundClient = false;
            boolean foundFacture = false;
            for (int r = 0; r <= summarySheet.getLastRowNum(); r++) {
                Row row = summarySheet.getRow(r);
                if (row != null && row.getCell(0) != null) {
                    String key = row.getCell(0).getStringCellValue();
                    if ("client_nom".equals(key)) {
                        foundClient = true;
                        assertThat(row.getCell(1).getStringCellValue()).isEqualTo("Acme Corporation");
                    }
                    if ("numero_facture".equals(key)) {
                        foundFacture = true;
                        assertThat(row.getCell(1).getStringCellValue()).isEqualTo("FAC-2026-001");
                    }
                }
            }
            assertThat(foundClient).isTrue();
            assertThat(foundFacture).isTrue();

            // Vérification de la feuille du tableau
            XSSFSheet tableSheet = workbook.getSheet("Lignes de Commande");
            assertThat(tableSheet).isNotNull();

            // En-têtes (ligne 2)
            Row headerRow = tableSheet.getRow(2);
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("Désignation");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("Quantité");
            assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("Prix Unitaire");

            // Première ligne de données (ligne 3)
            Row dataRow1 = tableSheet.getRow(3);
            assertThat(dataRow1.getCell(0).getStringCellValue()).isEqualTo("Licence Pro");
            assertThat(dataRow1.getCell(1).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(dataRow1.getCell(1).getNumericCellValue()).isEqualTo(5.0);
            assertThat(dataRow1.getCell(2).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(dataRow1.getCell(2).getNumericCellValue()).isEqualTo(120.50);

            // Ligne de total (ligne 5)
            Row totalRow = tableSheet.getRow(5);
            assertThat(totalRow).isNotNull();
            assertThat(totalRow.getCell(0).getStringCellValue()).isEqualTo("TOTAL");
            assertThat(totalRow.getCell(1).getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(totalRow.getCell(1).getCellFormula()).isEqualTo("SUM(B4:B5)");
            assertThat(totalRow.getCell(2).getCellType()).isEqualTo(CellType.FORMULA);
            assertThat(totalRow.getCell(2).getCellFormula()).isEqualTo("SUM(C4:C5)");
        }
    }

    @Test
    @DisplayName("Doit inférer les colonnes lorsque data contient des listes sans bloc design explicite")
    void shouldInferColumnsFromRawDataLists() throws IOException {
        ReportTemplate template = ReportTemplate.builder()
                .id(UUID.randomUUID())
                .nom("Rapport Libre")
                .statut(TemplateStatus.PUBLIE)
                .build();

        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> items = List.of(
                Map.of("code", "ITM-1", "score", 95.5),
                Map.of("code", "ITM-2", "score", 88.0)
        );
        data.put("resultats", items);

        byte[] excelBytes = excelRendererService.renderToExcel(template, data);
        assertThat(excelBytes).isNotNull();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excelBytes))) {
            XSSFSheet sheet = workbook.getSheet("resultats");
            assertThat(sheet).isNotNull();
            Row headerRow = sheet.getRow(2);
            assertThat(headerRow).isNotNull();
            assertThat(sheet.getLastRowNum()).isGreaterThanOrEqualTo(3);
        }
    }
}
