package com.rapports.moteur.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.entity.TemplateStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class RawDataExportServiceTest {

    private RawDataExportService exportService;
    private ReportTemplate template;

    @BeforeEach
    void setUp() {
        exportService = new RawDataExportService();
        template = ReportTemplate.builder()
                .id(UUID.randomUUID())
                .nom("Facture Vente")
                .codeEntreprise("ENT-001")
                .version(1)
                .statut(TemplateStatus.PUBLIE)
                .build();
    }

    @Test
    @DisplayName("Devrait exporter un tableau tabulaire en CSV avec BOM UTF-8 et point-virgule")
    void testExportTabularCsv() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("numero_facture", "FAC-2026-001");
        data.put("client", "Entreprise Alpha");

        List<Map<String, Object>> lignes = new ArrayList<>();
        Map<String, Object> l1 = new LinkedHashMap<>();
        l1.put("article", "Ordinateur Portable");
        l1.put("qte", 2);
        l1.put("prix", 850.50);
        lignes.add(l1);

        Map<String, Object> l2 = new LinkedHashMap<>();
        l2.put("article", "Écran 27 pouces");
        l2.put("qte", 1);
        l2.put("prix", 299.99);
        lignes.add(l2);

        data.put("lignes", lignes);

        byte[] csvBytes = exportService.exportToCsv(template, data, ';');

        // Vérifier le BOM UTF-8
        assertThat(csvBytes[0]).isEqualTo((byte) 0xEF);
        assertThat(csvBytes[1]).isEqualTo((byte) 0xBB);
        assertThat(csvBytes[2]).isEqualTo((byte) 0xBF);

        String csvContent = new String(csvBytes, 3, csvBytes.length - 3, StandardCharsets.UTF_8);
        assertThat(csvContent).contains("numero_facture;client;article;qte;prix");
        assertThat(csvContent).contains("FAC-2026-001;Entreprise Alpha;Ordinateur Portable;2;850.5");
        assertThat(csvContent).contains("FAC-2026-001;Entreprise Alpha;Écran 27 pouces;1;299.99");
    }

    @Test
    @DisplayName("Devrait respecter le délimiteur virgule pour l'export CSV")
    void testExportCsvWithComma() {
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", "ITEM-1");
        item.put("nom", "Clavier");
        data.put("items", List.of(item));

        byte[] csvBytes = exportService.exportToCsv(template, data, ',');
        String csvContent = new String(csvBytes, 3, csvBytes.length - 3, StandardCharsets.UTF_8);

        assertThat(csvContent).contains("id,nom");
        assertThat(csvContent).contains("ITEM-1,Clavier");
    }

    @Test
    @DisplayName("Devrait exporter des données simples clé-valeur en CSV")
    void testExportKeyValueCsv() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total_ht", 1200.0);
        data.put("tva", 240.0);
        data.put("total_ttc", 1440.0);

        byte[] csvBytes = exportService.exportToCsv(template, data, ';');
        String csvContent = new String(csvBytes, 3, csvBytes.length - 3, StandardCharsets.UTF_8);

        assertThat(csvContent).contains("Paramètre;Valeur");
        assertThat(csvContent).contains("Template;Facture Vente");
        assertThat(csvContent).contains("Entreprise;ENT-001");
        assertThat(csvContent).contains("total_ht;1200.0");
        assertThat(csvContent).contains("total_ttc;1440.0");
    }

    @Test
    @DisplayName("Devrait exporter les données en JSON indenté avec métadonnées")
    void testExportJson() throws Exception {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("client", "Tech Corp");
        data.put("lignes", List.of(Map.of("item", "Serveur", "prix", 5000)));

        byte[] jsonBytes = exportService.exportToJson(template, data);
        String jsonStr = new String(jsonBytes, StandardCharsets.UTF_8);

        ObjectMapper om = new ObjectMapper();
        JsonNode root = om.readTree(jsonStr);

        assertThat(root.has("templateId")).isTrue();
        assertThat(root.get("templateNom").asText()).isEqualTo("Facture Vente");
        assertThat(root.get("codeEntreprise").asText()).isEqualTo("ENT-001");
        assertThat(root.get("totalRecords").asInt()).isEqualTo(1);
        assertThat(root.get("donnees").get("client").asText()).isEqualTo("Tech Corp");
        assertThat(root.get("donnees").get("lignes").isArray()).isTrue();
    }
}
