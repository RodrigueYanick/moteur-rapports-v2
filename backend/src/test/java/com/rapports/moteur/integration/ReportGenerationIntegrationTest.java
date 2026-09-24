package com.rapports.moteur.integration;

import com.rapports.moteur.dto.dtoVariable.VariableRequest;
import com.rapports.moteur.entity.*;
import com.rapports.moteur.service.VariableService;
import com.rapports.moteur.service.rendering.GotenbergPdfRenderingEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Tests d'Intégration : Génération de Rapports (Synchrone, Asynchrone, Fallback Gotenberg)")
class ReportGenerationIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private GotenbergPdfRenderingEngine gotenbergEngine;

    @Autowired
    private VariableService variableService;

    private Entreprise testEntreprise;
    private User testUser;
    private String token;

    @BeforeEach
    void initContext() {
        testEntreprise = createEntreprise("PROD_CORP", "Production Corporation");
        testUser = createUser("operator@prod.com", "Password123!", Role.OPERATOR, testEntreprise);
        token = getBearerToken(testUser);
    }

    @Nested
    @DisplayName("1. Cas Nominaux (Happy Path)")
    class HappyPathTests {

        @Test
        @DisplayName("Génération synchrone de PDF sur un modèle publié avec insertion en base")
        void shouldGeneratePdfSynchronouslyAndRecordInDb() throws Exception {
            ReportTemplate template = createTemplate("Attestation Fiscale", "PROD_CORP", TemplateStatus.PUBLIE);
            template.setContenuDesign("{\"pages\":[{\"blocs\":[{\"type\":\"text\",\"contenu\":\"Bonjour {{nom_client}}\"}]}]}");
            templateRepository.save(template);

            // Configuration Gotenberg nominale
            when(gotenbergEngine.isAvailable()).thenReturn(true);
            when(gotenbergEngine.getEngineName()).thenReturn("gotenberg");
            when(gotenbergEngine.render(any(), any()))
                    .thenReturn("%PDF-1.4 Mocked Gotenberg PDF Content".getBytes());

            Map<String, Object> data = Map.of(
                    "nom_client", "Société Générale",
                    "montant", 50000
            );

            byte[] pdfBytes = mockMvc.perform(post("/api/templates/" + template.getId() + "/generate")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(data)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string("Content-Disposition", containsString(".pdf")))
                    .andReturn().getResponse().getContentAsByteArray();

            assertThat(pdfBytes).isNotEmpty();
            assertThat(new String(pdfBytes)).startsWith("%PDF-");

            // Vérification de la persistance de l'historique en base de données
            List<ReportGeneration> generations = generationRepository.findByTemplate_IdOrderByDateGenerationDesc(template.getId());
            assertThat(generations).isNotEmpty();
            ReportGeneration entry = generations.get(0);
            assertThat(entry.getTemplate().getId()).isEqualTo(template.getId());
            assertThat(entry.getDonneesRecues()).contains("Société Générale");
        }

        @Test
        @DisplayName("Génération asynchrone créant une tâche en file d'attente (202 Accepted)")
        void shouldInitiateAsyncGeneration() throws Exception {
            ReportTemplate template = createTemplate("Grand Livre Comptable", "PROD_CORP", TemplateStatus.PUBLIE);

            Map<String, Object> data = Map.of("exercice", 2026, "cloture", true);

            mockMvc.perform(post("/api/templates/" + template.getId() + "/generate-async")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(data)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.generationId").isNotEmpty())
                    .andExpect(jsonPath("$.status").value("EN_COURS"));

            List<ReportGeneration> generations = generationRepository.findByTemplate_IdOrderByDateGenerationDesc(template.getId());
            assertThat(generations).isNotEmpty();
        }

        @Test
        @DisplayName("Consultation de l'historique des générations d'un modèle")
        void shouldRetrieveGenerationHistory() throws Exception {
            ReportTemplate template = createTemplate("Bilan Social", "PROD_CORP", TemplateStatus.PUBLIE);

            // Création préalable d'une entrée d'historique en base
            ReportGeneration gen = ReportGeneration.builder()
                    .template(template)
                    .statut(GenerationStatus.SUCCES)
                    .donneesRecues("{\"annee\": 2026}")
                    .urlFichierGenere("reports/test.pdf")
                    .build();
            generationRepository.save(gen);

            mockMvc.perform(get("/api/generations")
                            .header("Authorization", token)
                            .param("templateId", template.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                    .andExpect(jsonPath("$[0].statut").value("SUCCES"));
        }
    }

    @Nested
    @DisplayName("2. Résilience & Intégration Externe (Fallback Gotenberg -> Flying Saucer)")
    class ExternalFaultToleranceTests {

        @Test
        @DisplayName("Bascule automatique (Fallback) vers Flying Saucer si Gotenberg renvoie une erreur 500")
        void shouldFallbackToFlyingSaucerWhenGotenbergFails() throws Exception {
            ReportTemplate template = createTemplate("Facture Tolérance Panne", "PROD_CORP", TemplateStatus.PUBLIE);
            template.setContenuDesign("{\"pages\":[{\"blocs\":[{\"type\":\"text\",\"contenu\":\"Facture de Secours\"}]}]}");
            templateRepository.save(template);

            // Simulation d'une panne du service externe Gotenberg (ex: Crash HTTP 500)
            when(gotenbergEngine.isAvailable()).thenReturn(true);
            when(gotenbergEngine.getEngineName()).thenReturn("gotenberg");
            when(gotenbergEngine.render(any(), any()))
                    .thenThrow(new RuntimeException("Gotenberg HTTP 500 Internal Server Error: Chromium crashed"));

            Map<String, Object> data = Map.of("reference", "FAC-999");

            // Même en cas d'erreur de Gotenberg, l'API ne doit PAS planter (200 OK grâce au fallback Flying Saucer)
            byte[] pdfBytes = mockMvc.perform(post("/api/templates/" + template.getId() + "/generate")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(data)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andReturn().getResponse().getContentAsByteArray();

            assertThat(pdfBytes).isNotEmpty();
            assertThat(new String(pdfBytes)).startsWith("%PDF-");
        }
    }

    @Nested
    @DisplayName("3. Cas aux Limites & Validation")
    class EdgeCasesTests {

        @Test
        @DisplayName("Rejet de génération sur un modèle en statut BROUILLON (400 Bad Request)")
        void shouldRejectGenerationOnDraftTemplate() throws Exception {
            ReportTemplate draft = createTemplate("Devis Non Publié", "PROD_CORP", TemplateStatus.BROUILLON);

            mockMvc.perform(post("/api/templates/" + draft.getId() + "/generate")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"client\": \"Test\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Le template doit etre publie avant generation")));
        }

        @Test
        @DisplayName("Génération sur un modèle avec un UUID inexistant -> 404 Not Found")
        void shouldReturn404WhenTemplateNotFoundForGeneration() throws Exception {
            UUID unknownId = UUID.randomUUID();

            mockMvc.perform(post("/api/templates/" + unknownId + "/generate")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"client\": \"Test\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("introuvable")));
        }

        @Test
        @DisplayName("Rejet si le corps de données n'est pas un objet JSON valide (400 Bad Request)")
        void shouldRejectInvalidJsonBodyForGeneration() throws Exception {
            ReportTemplate template = createTemplate("Template Test JSON", "PROD_CORP", TemplateStatus.PUBLIE);

            // Envoi d'un tableau JSON au lieu d'un objet clé/valeur
            mockMvc.perform(post("/api/templates/" + template.getId() + "/generate")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("[\"item1\", \"item2\"]"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("objet JSON")));
        }
    }
}
