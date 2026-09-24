package com.rapports.moteur.integration;

import com.rapports.moteur.dto.dtoTemplate.TemplateCreate;
import com.rapports.moteur.dto.dtoWorkspace.WorkspaceConfigRequest;
import com.rapports.moteur.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Tests d'Intégration : Configuration Feuille de Travail, Héritage Multi-Tenant et Gestion Globale des Erreurs")
class WorkspaceConfigAndErrorHandlingIntegrationTest extends BaseIntegrationTest {

    private Entreprise testEntreprise;
    private User testUser;
    private String token;

    @BeforeEach
    void initContext() {
        testEntreprise = createEntreprise("WORKSPACE_CORP", "Workspace Corporation");
        testUser = createUser("admin@workspace.com", "Password123!", Role.ADMIN_ENTREPRISE, testEntreprise);
        token = getBearerToken(testUser);
    }

    @Nested
    @DisplayName("1. Cas Nominaux (Happy Path & Héritage)")
    class HappyPathTests {

        @Test
        @DisplayName("Récupération de la configuration par défaut de l'entreprise (Paramètres d'usine)")
        void shouldReturnDefaultWorkspaceConfig() throws Exception {
            mockMvc.perform(get("/api/workspace-config")
                            .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.formatPapier").value("A4"))
                    .andExpect(jsonPath("$.margeGaucheMm").value(10))
                    .andExpect(jsonPath("$.margeDroiteMm").value(10))
                    .andExpect(jsonPath("$.couleurFond").value("#ffffff"));
        }

        @Test
        @DisplayName("Mise à jour de la configuration de travail et persistance en base")
        void shouldUpdateAndPersistWorkspaceConfig() throws Exception {
            WorkspaceConfigRequest request = WorkspaceConfigRequest.builder()
                    .formatPapier("A4")
                    .modePagination(PaginationMode.FIXED)
                    .margeGaucheMm(25)
                    .margeDroiteMm(25)
                    .margeHautMm(20)
                    .margeBasMm(20)
                    .couleurFond("#f1f5f9")
                    .headerActif(true)
                    .hauteurHeaderMm(18)
                    .headerContenu("En-tête Officiel")
                    .footerActif(true)
                    .hauteurFooterMm(12)
                    .numerotationPage(true)
                    .build();

            mockMvc.perform(put("/api/workspace-config")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.margeGaucheMm").value(25))
                    .andExpect(jsonPath("$.couleurFond").value("#f1f5f9"))
                    .andExpect(jsonPath("$.headerActif").value(true));

            // Vérification directe dans la base de données
            CompanyWorkspaceConfig persisted = workspaceConfigRepository.findByCodeEntreprise("WORKSPACE_CORP").orElseThrow();
            assertThat(persisted.getMargeGaucheMm()).isEqualTo(25);
            assertThat(persisted.getCouleurFond()).isEqualTo("#f1f5f9");
            assertThat(persisted.getHeaderActif()).isTrue();
        }

        @Test
        @DisplayName("Héritage automatique de la configuration d'entreprise lors de la création d'un nouveau modèle")
        void shouldInheritWorkspaceConfigWhenCreatingTemplate() throws Exception {
            // 1. Définir une configuration spécifique pour l'entreprise
            CompanyWorkspaceConfig config = CompanyWorkspaceConfig.builder()
                    .codeEntreprise("WORKSPACE_CORP")
                    .formatPapier("A4")
                    .modePagination(PaginationMode.FIXED)
                    .margeGaucheMm(30)
                    .margeDroiteMm(30)
                    .margeHautMm(25)
                    .margeBasMm(25)
                    .couleurFond("#e2e8f0")
                    .headerActif(true)
                    .hauteurHeaderMm(20)
                    .footerActif(true)
                    .hauteurFooterMm(15)
                    .numerotationPage(true)
                    .build();
            workspaceConfigRepository.save(config);

            // 2. Créer un template sans spécifier les marges ni le fond
            TemplateCreate templateReq = new TemplateCreate();
            templateReq.setNom("Rapport avec Héritage");
            templateReq.setCategorie(Categorie.AUTRES);

            mockMvc.perform(post("/api/templates")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(templateReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.margeGaucheMm").value(30))
                    .andExpect(jsonPath("$.couleurFond").value("#e2e8f0"))
                    .andExpect(jsonPath("$.headerActif").value(true));
        }

        @Test
        @DisplayName("Réinitialisation aux paramètres d'usine (/reset)")
        void shouldResetWorkspaceConfigToDefaults() throws Exception {
            // Configuration préalable personnalisée
            CompanyWorkspaceConfig customConfig = CompanyWorkspaceConfig.builder()
                    .codeEntreprise("WORKSPACE_CORP")
                    .margeGaucheMm(50)
                    .couleurFond("#000000")
                    .build();
            workspaceConfigRepository.save(customConfig);

            mockMvc.perform(post("/api/workspace-config/reset")
                            .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.margeGaucheMm").value(10))
                    .andExpect(jsonPath("$.couleurFond").value("#ffffff"));
        }
    }

    @Nested
    @DisplayName("2. Cas aux Limites & Validation (Edge Cases)")
    class EdgeCasesTests {

        @Test
        @DisplayName("Rejet : marges négatives ou supérieures à 100mm (400 Bad Request)")
        void shouldRejectInvalidMargins() throws Exception {
            WorkspaceConfigRequest invalidRequest = WorkspaceConfigRequest.builder()
                    .margeGaucheMm(-5) // Négatif
                    .margeDroiteMm(150) // Dépasse 100mm
                    .build();

            mockMvc.perform(put("/api/workspace-config")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message", anyOf(
                            containsString("marge"),
                            containsString("négative"),
                            containsString("dépasser")
                    )));
        }

        @Test
        @DisplayName("Rejet : hauteur d'en-tête inférieure au seuil minimal de 5mm (400 Bad Request)")
        void shouldRejectHeaderHeightBelowMinimum() throws Exception {
            WorkspaceConfigRequest invalidHeader = WorkspaceConfigRequest.builder()
                    .hauteurHeaderMm(2) // Minimum autorisé : 5mm
                    .build();

            mockMvc.perform(put("/api/workspace-config")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidHeader)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("3. Gestion des Erreurs et Robustesse de l'API (ProblemDetail)")
    class GlobalErrorHandlingTests {

        @Test
        @DisplayName("Sécurité : tentative d'accès anonyme à la configuration -> 401 Unauthorized")
        void shouldRejectAnonymousAccessToConfig() throws Exception {
            mockMvc.perform(get("/api/workspace-config"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Formatage des erreurs de validation selon le DTO ApiError")
        void shouldFormatValidationErrorsConsistently() throws Exception {
            TemplateCreate invalid = new TemplateCreate();
            invalid.setNom(""); // Blank invalide

            mockMvc.perform(post("/api/templates")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Bad Request"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }
    }
}

