package com.rapports.moteur.integration;

import com.rapports.moteur.entity.*;
import com.rapports.moteur.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Tests d'Intégration : Multi-Formats d'Export Enrichis (PNG/JPEG HD, CSV, JSON)")
class MultiFormatExportIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private DocumentRepository documentRepository;

    private Entreprise testEntreprise;
    private User testUser;
    private String token;
    private ReportTemplate template;

    @BeforeEach
    void setUp() {
        testEntreprise = createEntreprise("ENT-EXPORT", "Export Corp");
        testUser = createUser("export_user@export.com", "Password123!", Role.ADMIN_ENTREPRISE, testEntreprise);
        token = getBearerToken(testUser);

        template = createTemplate("Rapport Ventes Mensuel", "ENT-EXPORT", TemplateStatus.PUBLIE);
        template.setContenuDesign("{\"pages\":[{\"blocs\":[{\"type\":\"text\",\"contenu\":\"Facture pour {{client}}\"}]}]}");
        templateRepository.save(template);
    }

    @Test
    @DisplayName("Devrait exporter le rapport en image PNG HD (300 DPI)")
    void testExportImagePng() throws Exception {
        Map<String, Object> data = Map.of(
                "client", "Acme Corporation",
                "total", 1500.0
        );

        byte[] pngBytes = mockMvc.perform(post("/api/templates/" + template.getId() + "/export-image")
                        .header("Authorization", token)
                        .header("X-Entreprise-Code", "ENT-EXPORT")
                        .param("format", "PNG")
                        .param("dpi", "300")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Content-Disposition", containsString("rapport.png")))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pngBytes).isNotEmpty();
        // Magic bytes PNG
        assertThat(pngBytes[0]).isEqualTo((byte) 0x89);
        assertThat(pngBytes[1]).isEqualTo((byte) 'P');
        assertThat(pngBytes[2]).isEqualTo((byte) 'N');
        assertThat(pngBytes[3]).isEqualTo((byte) 'G');
    }

    @Test
    @DisplayName("Devrait exporter le rapport en image JPEG HD")
    void testExportImageJpeg() throws Exception {
        Map<String, Object> data = Map.of(
                "client", "Acme Corporation",
                "total", 1500.0
        );

        byte[] jpegBytes = mockMvc.perform(post("/api/templates/" + template.getId() + "/export-image")
                        .header("Authorization", token)
                        .header("X-Entreprise-Code", "ENT-EXPORT")
                        .param("format", "JPEG")
                        .param("dpi", "150")
                        .param("quality", "0.90")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("Content-Disposition", containsString("rapport.jpg")))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(jpegBytes).isNotEmpty();
        // Magic bytes JPEG
        assertThat(jpegBytes[0]).isEqualTo((byte) 0xFF);
        assertThat(jpegBytes[1]).isEqualTo((byte) 0xD8);
        assertThat(jpegBytes[2]).isEqualTo((byte) 0xFF);
    }

    @Test
    @DisplayName("Devrait exporter les données calculées en CSV avec BOM UTF-8")
    void testExportCsv() throws Exception {
        Map<String, Object> data = Map.of(
                "numero_facture", "F-2026-99",
                "lignes", List.of(
                        Map.of("produit", "Licence Logiciel", "quantite", 5, "prix", 120.0),
                        Map.of("produit", "Support Annuel", "quantite", 1, "prix", 500.0)
                )
        );

        byte[] csvBytes = mockMvc.perform(post("/api/templates/" + template.getId() + "/export-csv")
                        .header("Authorization", token)
                        .header("X-Entreprise-Code", "ENT-EXPORT")
                        .param("delimiter", ";")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", containsString("rapport.csv")))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(csvBytes).isNotEmpty();
        // BOM UTF-8
        assertThat(csvBytes[0]).isEqualTo((byte) 0xEF);
        assertThat(csvBytes[1]).isEqualTo((byte) 0xBB);
        assertThat(csvBytes[2]).isEqualTo((byte) 0xBF);

        String csvText = new String(csvBytes, 3, csvBytes.length - 3, StandardCharsets.UTF_8);
        assertThat(csvText).contains("numero_facture");
        assertThat(csvText).contains("produit");
        assertThat(csvText).contains("Licence Logiciel");
        assertThat(csvText).contains("Support Annuel");
    }

    @Test
    @DisplayName("Devrait exporter les données calculées en JSON brut enrichi")
    void testExportJson() throws Exception {
        Map<String, Object> data = Map.of(
                "client", "Global Tech",
                "commandes", List.of(
                        Map.of("id", "CMD-1", "statut", "LIVRÉ")
                )
        );

        mockMvc.perform(post("/api/templates/" + template.getId() + "/export-json")
                        .header("Authorization", token)
                        .header("X-Entreprise-Code", "ENT-EXPORT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(header().string("Content-Disposition", containsString("rapport_data.json")))
                .andExpect(jsonPath("$.templateNom").value("Rapport Ventes Mensuel"))
                .andExpect(jsonPath("$.codeEntreprise").value("ENT-EXPORT"))
                .andExpect(jsonPath("$.totalRecords").value(1))
                .andExpect(jsonPath("$.donnees.client").value("Global Tech"));
    }

    @Test
    @DisplayName("Devrait exporter un document sauvegardé existant en image et en CSV")
    void testExportExistingDocument() throws Exception {
        Map<String, Object> docData = Map.of(
                "client", "Client Existant",
                "articles", List.of(Map.of("nom", "Stylo", "prix", 2.5))
        );

        Document doc = Document.builder()
                .template(template)
                .codeEntreprise("ENT-EXPORT")
                .nom("Facture_Client_Existant")
                .donnees(objectMapper.writeValueAsString(docData))
                .statut(StatutDocument.BROUILLON)
                .build();
        doc = documentRepository.save(doc);

        // Export Document en CSV
        mockMvc.perform(get("/api/templates/" + template.getId() + "/documents/" + doc.getId() + "/export-csv")
                        .header("Authorization", token)
                        .header("X-Entreprise-Code", "ENT-EXPORT"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", containsString("Facture_Client_Existant.csv")));

        // Export Document en Image
        mockMvc.perform(get("/api/templates/" + template.getId() + "/documents/" + doc.getId() + "/export-image")
                        .header("Authorization", token)
                        .header("X-Entreprise-Code", "ENT-EXPORT")
                        .param("format", "PNG")
                        .param("dpi", "150"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Content-Disposition", containsString("Facture_Client_Existant.png")));
    }

    @Test
    @DisplayName("Devrait rejeter un export image avec un DPI hors bornes")
    void testRejectInvalidDpi() throws Exception {
        Map<String, Object> data = Map.of("client", "Test");

        mockMvc.perform(post("/api/templates/" + template.getId() + "/export-image")
                        .header("Authorization", token)
                        .header("X-Entreprise-Code", "ENT-EXPORT")
                        .param("dpi", "20")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("DPI")));
    }
}
