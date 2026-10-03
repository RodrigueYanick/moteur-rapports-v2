package com.rapports.moteur.integration;

import com.rapports.moteur.dto.dtoAi.AiMockDataRequest;
import com.rapports.moteur.dto.dtoAi.AiTemplatePromptRequest;
import com.rapports.moteur.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Tests d'Intégration : Assistant IA (Génération de Modèles & Smart Mock Data)")
class AiAssistantIntegrationTest extends BaseIntegrationTest {

    private Entreprise testEntreprise;
    private User testUser;
    private String token;

    @BeforeEach
    void setUpTestData() {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        testEntreprise = createEntreprise("ENT-AI-" + uniqueSuffix, "Entreprise AI Test");
        testUser = createUser("designer-" + uniqueSuffix + "@test.com", "Password123!", Role.DESIGNER, testEntreprise);
        token = getBearerToken(testUser);
    }

    @Test
    @DisplayName("Génération d'un modèle par IA : création effective en base de données et extraction des variables")
    void testGenerateTemplateIntegration() throws Exception {
        AiTemplatePromptRequest request = AiTemplatePromptRequest.builder()
                .prompt("Génère-moi un devis pour un artisan électricien avec tableau de matériel et taux horaire")
                .nom("Devis Électricien IA")
                .categorie("VENTES")
                .formatPapier("A4")
                .build();

        String responseContent = mockMvc.perform(post("/api/ai/generate-template")
                        .header("Authorization", token)
                        .header("X-Entreprise-Code", testEntreprise.getCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("succès")))
                .andExpect(jsonPath("$.data.nom").value("Devis Électricien IA"))
                .andExpect(jsonPath("$.data.categorie").value("VENTES"))
                .andExpect(jsonPath("$.data.formatPapier").value("A4"))
                .andExpect(jsonPath("$.data.templateId").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        // Extraire l'UUID du template généré et vérifier en base
        var jsonNode = objectMapper.readTree(responseContent);
        UUID createdId = UUID.fromString(jsonNode.path("data").path("templateId").asText());

        var optTemplate = templateRepository.findById(createdId);
        assertThat(optTemplate).isPresent();
        ReportTemplate template = optTemplate.get();
        assertThat(template.getStatut()).isEqualTo(TemplateStatus.BROUILLON);
        assertThat(template.getVersion()).isEqualTo(1);
        assertThat(template.getCodeEntreprise()).isEqualTo(testEntreprise.getCode());

        // Vérifier que les variables ont bien été enregistrées en base
        List<ReportVariable> variables = variableRepository.findByTemplate_Id(createdId);
        assertThat(variables).isNotEmpty();
    }

    @Test
    @DisplayName("Génération de Smart Mock Data par IA")
    void testGenerateMockDataIntegration() throws Exception {
        AiMockDataRequest request = AiMockDataRequest.builder()
                .templateNom("Facture de Travaux")
                .variables(List.of(
                        new AiMockDataRequest.VariableItem("numero_document", "STRING", "Numéro de document"),
                        new AiMockDataRequest.VariableItem("client_nom", "STRING", "Nom du client"),
                        new AiMockDataRequest.VariableItem("total_ht", "FLOAT", "Montant total HT")
                ))
                .arrayColumns(List.of("designation", "quantite", "prix_unitaire", "total_ligne"))
                .rowCount(3)
                .build();

        mockMvc.perform(post("/api/ai/mock-data")
                        .header("Authorization", token)
                        .header("X-Entreprise-Code", testEntreprise.getCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("succès")))
                .andExpect(jsonPath("$.data.data.numero_document").isNotEmpty())
                .andExpect(jsonPath("$.data.data.total_ht").isNotEmpty());
    }

    @Test
    @DisplayName("Statut du service IA disponible pour utilisateur authentifié")
    void testGetAiStatus() throws Exception {
        mockMvc.perform(get("/api/ai/status")
                        .header("Authorization", token)
                        .header("X-Entreprise-Code", testEntreprise.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").isNotEmpty())
                .andExpect(jsonPath("$.data.model").isNotEmpty());
    }

    @Test
    @DisplayName("Accès non authentifié refusé avec HTTP 401")
    void testUnauthenticatedAccessDenied() throws Exception {
        mockMvc.perform(post("/api/ai/generate-template")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"Créer un devis\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/ai/mock-data")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/ai/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Génération d'un template de distribution avec prompt complexe et sauvegarde dataSourceMapping")
    void testGenerateTemplateComplexDistributionPrompt() throws Exception {
        String prompt = """
            Crée un template de rapport professionnel et réutilisable pour une entreprise de distribution au Cameroun.
            Titre : « RAPPORT MENSUEL DES VENTES »
            Format A4 avec en-tête, résumé 4 indicateurs, tableau des ventes, synthèse financière FCFA.
            """;

        AiTemplatePromptRequest request = AiTemplatePromptRequest.builder()
                .prompt(prompt)
                .nom("Rapport Mensuel des Ventes Cameroun")
                .categorie("VENTES")
                .formatPapier("A4")
                .build();

        String responseContent = mockMvc.perform(post("/api/ai/generate-template")
                        .header("Authorization", token)
                        .header("X-Entreprise-Code", testEntreprise.getCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("succès")))
                .andExpect(jsonPath("$.data.templateId").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        var jsonNode = objectMapper.readTree(responseContent);
        UUID createdId = UUID.fromString(jsonNode.path("data").path("templateId").asText());

        var optTemplate = templateRepository.findById(createdId);
        assertThat(optTemplate).isPresent();
        ReportTemplate template = optTemplate.get();

        // Tester la mise à jour de data_source_mapping avec JSONB
        template.setDataSourceMapping("{\"col1\":\"var1\",\"col2\":\"var2\"}");
        ReportTemplate updated = templateRepository.save(template);
        assertThat(updated.getDataSourceMapping()).isEqualTo("{\"col1\":\"var1\",\"col2\":\"var2\"}");

        // Tester la normalisation automatique à null si chaîne vide
        updated.setDataSourceMapping("");
        ReportTemplate cleared = templateRepository.save(updated);
        assertThat(cleared.getDataSourceMapping()).isNull();
    }
}

