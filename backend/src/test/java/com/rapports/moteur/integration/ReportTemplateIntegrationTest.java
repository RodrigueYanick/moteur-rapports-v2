package com.rapports.moteur.integration;

import com.rapports.moteur.dto.dtoTemplate.TemplateCreate;
import com.rapports.moteur.dto.dtoVariable.VariableRequest;
import com.rapports.moteur.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Tests d'Intégration : Cycle de Vie Complet des Modèles de Rapports (Templates)")
class ReportTemplateIntegrationTest extends BaseIntegrationTest {

    private Entreprise testEntreprise;
    private User testUser;
    private String token;

    @BeforeEach
    void initContext() {
        testEntreprise = createEntreprise("ALPHA_CORP", "Alpha Corporation");
        testUser = createUser("designer@alpha.com", "SecretPass123!", Role.DESIGNER, testEntreprise);
        token = getBearerToken(testUser);
    }

    @Nested
    @DisplayName("1. Cas Nominaux (Happy Path)")
    class HappyPathTests {

        @Test
        @DisplayName("Création d'un modèle A4 avec mise en page et persistance réelle en base")
        void shouldCreateStandardTemplateSuccessfully() throws Exception {
            TemplateCreate request = new TemplateCreate();
            request.setNom("Facture Standard Pro");
            request.setDescription("Modèle de facturation mensuelle");
            request.setCategorie(Categorie.VENTES);
            request.setFormatPapier("A4");
            request.setModePagination(PaginationMode.FIXED);
            request.setMargeGaucheMm(15);
            request.setMargeDroiteMm(15);
            request.setMargeHautMm(15);
            request.setMargeBasMm(15);
            request.setContenuDesign("{\"pages\":[{\"elements\":[]}]}");

            String responseBody = mockMvc.perform(post("/api/templates")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.nom").value("Facture Standard Pro"))
                    .andExpect(jsonPath("$.statut").value("BROUILLON"))
                    .andExpect(jsonPath("$.version").value(1))
                    .andExpect(jsonPath("$.formatPapier").value("A4"))
                    .andExpect(jsonPath("$.margeGaucheMm").value(15))
                    .andReturn().getResponse().getContentAsString();

            UUID createdId = UUID.fromString(objectMapper.readTree(responseBody).get("id").asText());

            // Vérification directe dans la base de données
            ReportTemplate saved = templateRepository.findById(createdId).orElse(null);
            assertThat(saved).isNotNull();
            assertThat(saved.getNom()).isEqualTo("Facture Standard Pro");
            assertThat(saved.getCodeEntreprise()).isEqualTo("ALPHA_CORP");
            assertThat(saved.getStatut()).isEqualTo(TemplateStatus.BROUILLON);
        }

        @Test
        @DisplayName("Création d'un modèle de format CUSTOM avec dimensions valides")
        void shouldCreateCustomFormatTemplate() throws Exception {
            TemplateCreate request = new TemplateCreate();
            request.setNom("Ticket Caisse Reçu");
            request.setCategorie(Categorie.VENTES);
            request.setFormatPapier("CUSTOM");
            request.setLargeurMm(80);
            request.setHauteurMm(200);
            request.setModePagination(PaginationMode.AUTO);

            mockMvc.perform(post("/api/templates")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.formatPapier").value("CUSTOM"))
                    .andExpect(jsonPath("$.largeurMm").value(80))
                    .andExpect(jsonPath("$.hauteurMm").value(200));
        }

        @Test
        @DisplayName("Cycle de publication, archivage et création de version")
        void shouldHandleFullTemplateLifecycle() throws Exception {
            ReportTemplate template = createTemplate("Bon de Livraison", "ALPHA_CORP", TemplateStatus.BROUILLON);

            // 1. Publication
            mockMvc.perform(post("/api/templates/" + template.getId() + "/publish")
                            .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("PUBLIE"));

            ReportTemplate published = templateRepository.findById(template.getId()).orElseThrow();
            assertThat(published.getStatut()).isEqualTo(TemplateStatus.PUBLIE);

            // 2. Archivage
            mockMvc.perform(post("/api/templates/" + template.getId() + "/archive")
                            .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statut").value("ARCHIVE"));

            // 3. Nouvelle version issue du modèle archivé
            String newVersionBody = mockMvc.perform(post("/api/templates/" + template.getId() + "/new-version")
                            .header("Authorization", token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.version").value(2))
                    .andExpect(jsonPath("$.statut").value("BROUILLON"))
                    .andExpect(jsonPath("$.parentTemplateId").value(template.getId().toString()))
                    .andReturn().getResponse().getContentAsString();

            UUID newVersionId = UUID.fromString(objectMapper.readTree(newVersionBody).get("id").asText());
            ReportTemplate v2 = templateRepository.findById(newVersionId).orElseThrow();
            assertThat(v2.getVersion()).isEqualTo(2);
            assertThat(v2.getParentTemplate().getId()).isEqualTo(template.getId());

            // 4. Arbre des versions
            mockMvc.perform(get("/api/templates/" + template.getId() + "/versions")
                            .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.chronologie", hasSize(2)));
        }

        @Test
        @DisplayName("Gestion des variables : ajout, mise à jour et suppression")
        void shouldManageVariablesOnDraftTemplate() throws Exception {
            ReportTemplate template = createTemplate("Rapport RH", "ALPHA_CORP", TemplateStatus.BROUILLON);

            // 1. Ajout de variable
            VariableRequest varReq = new VariableRequest();
            varReq.setNomVariable("collaborateur_nom");
            varReq.setType(VariableType.STRING);
            varReq.setObligatoire(true);
            varReq.setDescription("Nom complet de l'employé");

            String varResponse = mockMvc.perform(post("/api/templates/" + template.getId() + "/variables")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(varReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nomVariable").value("collaborateur_nom"))
                    .andReturn().getResponse().getContentAsString();

            UUID varId = UUID.fromString(objectMapper.readTree(varResponse).get("id").asText());
            assertThat(variableRepository.findById(varId)).isPresent();

            // 2. Mise à jour de la variable
            varReq.setDescription("Nom de famille et prénom");
            mockMvc.perform(put("/api/templates/" + template.getId() + "/variables/" + varId)
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(varReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value("Nom de famille et prénom"));

            // 3. Suppression de la variable
            mockMvc.perform(delete("/api/templates/" + template.getId() + "/variables/" + varId)
                            .header("Authorization", token))
                    .andExpect(status().isNoContent());

            assertThat(variableRepository.findById(varId)).isEmpty();
        }

        @Test
        @DisplayName("Suppression d'un modèle brouillon et nettoyage complet")
        void shouldDeleteDraftTemplateSuccessfully() throws Exception {
            ReportTemplate template = createTemplate("Devis Temporaire", "ALPHA_CORP", TemplateStatus.BROUILLON);

            mockMvc.perform(delete("/api/templates/" + template.getId())
                            .header("Authorization", token))
                    .andExpect(status().isNoContent());

            assertThat(templateRepository.findById(template.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("2. Cas aux Limites & Validation (Edge Cases)")
    class EdgeCasesTests {

        @Test
        @DisplayName("Échec : nom de modèle vide ou absent (400 Bad Request)")
        void shouldRejectTemplateWithEmptyName() throws Exception {
            TemplateCreate request = new TemplateCreate();
            request.setNom(""); // Blank
            request.setCategorie(Categorie.FINANCE);

            mockMvc.perform(post("/api/templates")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message", containsString("nom")));
        }

        @Test
        @DisplayName("Échec : format CUSTOM sans dimensions ou avec dimensions négatives (400 Bad Request)")
        void shouldRejectCustomFormatWithoutValidDimensions() throws Exception {
            TemplateCreate request = new TemplateCreate();
            request.setNom("Badge Événement");
            request.setFormatPapier("CUSTOM");
            request.setLargeurMm(-10); // Invalide
            request.setHauteurMm(0);   // Invalide

            mockMvc.perform(post("/api/templates")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("dimensions personnalisées")));
        }

        @Test
        @DisplayName("Échec : tentative d'ajout de variable en doublon sur le même template (400 Bad Request)")
        void shouldRejectDuplicateVariable() throws Exception {
            ReportTemplate template = createTemplate("Inventaire Stock", "ALPHA_CORP", TemplateStatus.BROUILLON);

            VariableRequest varReq = new VariableRequest();
            varReq.setNomVariable("quantite");
            varReq.setType(VariableType.FLOAT);
            varReq.setObligatoire(true);

            // Premier ajout : OK
            mockMvc.perform(post("/api/templates/" + template.getId() + "/variables")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(varReq)))
                    .andExpect(status().isCreated());

            // Deuxième ajout avec le même nom : Rejet
            mockMvc.perform(post("/api/templates/" + template.getId() + "/variables")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(varReq)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("existe déjà")));
        }

        @Test
        @DisplayName("Échec : tentative de modification de variable sur un modèle publié (400 Bad Request)")
        void shouldRejectVariableModificationOnPublishedTemplate() throws Exception {
            ReportTemplate template = createTemplate("Certificat ISO", "ALPHA_CORP", TemplateStatus.PUBLIE);

            VariableRequest varReq = new VariableRequest();
            varReq.setNomVariable("norme");
            varReq.setType(VariableType.STRING);

            mockMvc.perform(post("/api/templates/" + template.getId() + "/variables")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(varReq)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("statut BROUILLON")));
        }
    }

    @Nested
    @DisplayName("3. Gestion des Erreurs : Identifiants Inexistants et Invalides")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Recherche d'un modèle avec un UUID inexistant -> 404 Not Found")
        void shouldReturn404WhenTemplateNotFound() throws Exception {
            UUID randomId = UUID.randomUUID();

            mockMvc.perform(get("/api/templates/" + randomId)
                            .header("Authorization", token))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message", containsString("introuvable")));
        }

        @Test
        @DisplayName("Requête avec un UUID syntaxiquement invalide -> 400 Bad Request (TypeMismatch)")
        void shouldReturn400WhenUuidMalformed() throws Exception {
            mockMvc.perform(get("/api/templates/not-a-valid-uuid-12345")
                            .header("Authorization", token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message", containsString("Paramètre invalide")));
        }
    }
}
