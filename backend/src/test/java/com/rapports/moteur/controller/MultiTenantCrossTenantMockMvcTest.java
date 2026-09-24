package com.rapports.moteur.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.dtoDocument.DocumentCreate;
import com.rapports.moteur.dto.dtoTemplate.TemplateCreate;
import com.rapports.moteur.dto.dtoVariable.VariableRequest;
import com.rapports.moteur.entity.*;
import com.rapports.moteur.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Sprint 1 - Tests d'intégration MockMvc de cloisonnement Multi-Tenant réel")
class MultiTenantCrossTenantMockMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReportTemplateRepository templateRepository;

    @Autowired
    private ReportVariableRepository variableRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ReportBatchRepository batchRepository;

    private static final String TENANT_A = "ENT_ALPHA";
    private static final String TENANT_B = "ENT_BETA";

    private ReportTemplate templateA;
    private ReportTemplate templatePublic;
    private Document documentA;
    private ReportBatch batchA;

    @BeforeEach
    void setUp() {
        batchRepository.deleteAll();
        documentRepository.deleteAll();
        variableRepository.deleteAll();
        templateRepository.deleteAll();

        // 1. Template privé pour l'entreprise ALPHA
        templateA = ReportTemplate.builder()
                .nom("Facture Vente Alpha")
                .description("Modèle privé entreprise Alpha")
                .codeEntreprise(TENANT_A)
                .statut(TemplateStatus.BROUILLON)
                .version(1)
                .formatPapier("A4")
                .modePagination(PaginationMode.FIXED)
                .contenuDesign("{\"blocs\":[]}")
                .schema("[]")
                .dateCreation(LocalDateTime.now())
                .build();
        templateA = templateRepository.save(templateA);

        // Variable sur template A
        ReportVariable varA = ReportVariable.builder()
                .template(templateA)
                .nomVariable("montant_total")
                .type(VariableType.FLOAT)
                .obligatoire(true)
                .description("Montant HT")
                .build();
        variableRepository.save(varA);

        // 2. Template public accessible à tous
        templatePublic = ReportTemplate.builder()
                .nom("Certificat Standard Public")
                .description("Modèle public sans entreprise")
                .codeEntreprise(null) // public
                .statut(TemplateStatus.PUBLIE)
                .version(1)
                .formatPapier("A4")
                .modePagination(PaginationMode.FIXED)
                .contenuDesign("{\"blocs\":[]}")
                .schema("[]")
                .dateCreation(LocalDateTime.now())
                .build();
        templatePublic = templateRepository.save(templatePublic);

        // 3. Document appartenant à l'entreprise ALPHA
        documentA = Document.builder()
                .nom("Facture_ALPHA_1001")
                .template(templateA)
                .codeEntreprise(TENANT_A)
                .statut(StatutDocument.BROUILLON)
                .donnees("{\"montant_total\": 1500}")
                .dateCreation(LocalDateTime.now())
                .build();
        documentA = documentRepository.save(documentA);

        // 4. Lot de génération appartenant à l'entreprise ALPHA
        batchA = ReportBatch.builder()
                .template(templatePublic)
                .codeEntreprise(TENANT_A)
                .statut(BatchStatus.EN_ATTENTE)
                .totalItems(1)
                .dateCreation(LocalDateTime.now())
                .build();
        batchA = batchRepository.save(batchA);
    }

    // =========================================================================
    // SECTION 1 : Cloisonnement des TEMPLATES
    // =========================================================================
    @Nested
    @DisplayName("Cloisonnement des Modèles (Templates)")
    class TemplateIsolationTests {

        @Test
        @WithMockUser
        @DisplayName("L'entreprise ALPHA voit son template privé dans la liste")
        void testTenantASeesItsOwnTemplate() throws Exception {
            mockMvc.perform(get("/api/templates")
                            .header("X-Entreprise-Code", TENANT_A))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                    .andExpect(jsonPath("$[*].nom", hasItem("Facture Vente Alpha")));
        }

        @Test
        @WithMockUser
        @DisplayName("L'entreprise BETA ne voit PAS le template privé de l'entreprise ALPHA")
        void testTenantBCannotSeeTenantATemplateInList() throws Exception {
            mockMvc.perform(get("/api/templates")
                            .header("X-Entreprise-Code", TENANT_B))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].nom", not(hasItem("Facture Vente Alpha"))))
                    .andExpect(jsonPath("$[*].nom", hasItem("Certificat Standard Public")));
        }

        @Test
        @WithMockUser
        @DisplayName("L'entreprise BETA reçoit 404 lorsqu'elle demande directement le template privé de ALPHA")
        void testTenantBCannotGetTenantATemplateById() throws Exception {
            mockMvc.perform(get("/api/templates/" + templateA.getId())
                            .header("X-Entreprise-Code", TENANT_B))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("L'entreprise BETA reçoit 404 en tentant de modifier le template de ALPHA")
        void testTenantBCannotUpdateTenantATemplate() throws Exception {
            TemplateCreate updateDto = new TemplateCreate();
            updateDto.setNom("Piratage par Beta");

            mockMvc.perform(put("/api/templates/" + templateA.getId())
                            .header("X-Entreprise-Code", TENANT_B)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("L'entreprise BETA reçoit 404 en tentant de supprimer le template de ALPHA")
        void testTenantBCannotDeleteTenantATemplate() throws Exception {
            mockMvc.perform(delete("/api/templates/" + templateA.getId())
                            .header("X-Entreprise-Code", TENANT_B))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("Un appelant sans header reçoit 404 lorsqu'il cible un template privé")
        void testNoHeaderCannotAccessPrivateTemplate() throws Exception {
            mockMvc.perform(get("/api/templates/" + templateA.getId()))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // SECTION 2 : Cloisonnement des VARIABLES
    // =========================================================================
    @Nested
    @DisplayName("Cloisonnement des Variables de Modèles")
    class VariableIsolationTests {

        @Test
        @WithMockUser
        @DisplayName("L'entreprise ALPHA accède aux variables de son modèle")
        void testTenantAAccessesItsTemplateVariables() throws Exception {
            mockMvc.perform(get("/api/templates/" + templateA.getId() + "/variables")
                            .header("X-Entreprise-Code", TENANT_A))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].nomVariable", is("montant_total")));
        }

        @Test
        @WithMockUser
        @DisplayName("L'entreprise BETA reçoit 404 en tentant de lister les variables du modèle de ALPHA")
        void testTenantBCannotListVariablesOfTenantA() throws Exception {
            mockMvc.perform(get("/api/templates/" + templateA.getId() + "/variables")
                            .header("X-Entreprise-Code", TENANT_B))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("L'entreprise BETA reçoit 404 en tentant d'ajouter une variable au modèle de ALPHA")
        void testTenantBCannotAddVariableToTenantA() throws Exception {
            VariableRequest newVar = new VariableRequest();
            newVar.setNomVariable("variable_espionne");
            newVar.setType(VariableType.STRING);
            newVar.setObligatoire(false);

            mockMvc.perform(post("/api/templates/" + templateA.getId() + "/variables")
                            .header("X-Entreprise-Code", TENANT_B)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newVar)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("Un appelant sans header reçoit 404 sur les variables d'un modèle privé")
        void testNoHeaderCannotAccessVariablesOfPrivateTemplate() throws Exception {
            mockMvc.perform(get("/api/templates/" + templateA.getId() + "/variables"))
                    .andExpect(status().isNotFound());
        }
    }

    // =========================================================================
    // SECTION 3 : Cloisonnement des DOCUMENTS
    // =========================================================================
    @Nested
    @DisplayName("Cloisonnement des Documents")
    class DocumentIsolationTests {

        @Test
        @WithMockUser
        @DisplayName("L'entreprise ALPHA voit son document dans la liste globale")
        void testTenantASeesItsDocumentInGlobalList() throws Exception {
            mockMvc.perform(get("/api/documents")
                            .header("X-Entreprise-Code", TENANT_A))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].nom", hasItem("Facture_ALPHA_1001")));
        }

        @Test
        @WithMockUser
        @DisplayName("L'entreprise BETA ne voit PAS le document de l'entreprise ALPHA dans la liste globale")
        void testTenantBCannotSeeTenantADocumentInGlobalList() throws Exception {
            mockMvc.perform(get("/api/documents")
                            .header("X-Entreprise-Code", TENANT_B))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].nom", not(hasItem("Facture_ALPHA_1001"))));
        }

        @Test
        @WithMockUser
        @DisplayName("L'entreprise BETA ne voit pas les documents de ALPHA via l'endpoint par template")
        void testTenantBCannotSeeTenantADocumentsByTemplate() throws Exception {
            mockMvc.perform(get("/api/templates/" + templateA.getId() + "/documents")
                            .header("X-Entreprise-Code", TENANT_B))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("L'entreprise BETA est rejetée lorsqu'elle tente de lire le document de ALPHA par ID")
        void testTenantBCannotGetDocumentById() throws Exception {
            mockMvc.perform(get("/api/templates/" + templateA.getId() + "/documents/" + documentA.getId())
                            .header("X-Entreprise-Code", TENANT_B))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("L'entreprise BETA est rejetée lorsqu'elle tente de supprimer le document de ALPHA")
        void testTenantBCannotDeleteDocumentOfTenantA() throws Exception {
            mockMvc.perform(delete("/api/templates/" + templateA.getId() + "/documents/" + documentA.getId())
                            .header("X-Entreprise-Code", TENANT_B))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("Un appelant sans header reçoit une liste vide de documents et ne peut rien créer")
        void testNoHeaderReturnsEmptyListAndRejectsCreate() throws Exception {
            // Liste vide
            mockMvc.perform(get("/api/documents"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));

            // Création rejetée
            DocumentCreate docCreate = new DocumentCreate();
            docCreate.setNom("DocSansTenant");
            docCreate.setDonnees(Map.of());

            mockMvc.perform(post("/api/templates/" + templatePublic.getId() + "/documents")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(docCreate)))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // SECTION 4 : Cloisonnement des GÉNÉRATIONS & LOTS (BATCHES)
    // =========================================================================
    @Nested
    @DisplayName("Cloisonnement des Générations et Traitements par Lot")
    class GenerationAndBatchIsolationTests {

        @Test
        @WithMockUser
        @DisplayName("L'entreprise BETA reçoit 404 sur l'historique des générations du template de ALPHA")
        void testTenantBCannotAccessGenerationHistoryOfTenantA() throws Exception {
            mockMvc.perform(get("/api/generations?templateId=" + templateA.getId())
                            .header("X-Entreprise-Code", TENANT_B))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("L'entreprise BETA reçoit 404 en tentant de générer un PDF sur le template de ALPHA")
        void testTenantBCannotGeneratePdfFromTenantATemplate() throws Exception {
            mockMvc.perform(post("/api/templates/" + templateA.getId() + "/generate")
                            .header("X-Entreprise-Code", TENANT_B)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"montant_total\": 100}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("L'entreprise BETA ne voit pas les lots de génération de l'entreprise ALPHA")
        void testTenantBCannotSeeTenantABatches() throws Exception {
            mockMvc.perform(get("/api/batches")
                            .header("X-Entreprise-Code", TENANT_B))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        @WithMockUser
        @DisplayName("L'entreprise BETA ne peut pas consulter le lot de l'entreprise ALPHA par ID")
        void testTenantBCannotGetBatchOfTenantA() throws Exception {
            mockMvc.perform(get("/api/batches/" + batchA.getId())
                            .header("X-Entreprise-Code", TENANT_B))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @WithMockUser
        @DisplayName("Un appelant sans header reçoit une liste vide de lots")
        void testNoHeaderBatchesReturnsEmpty() throws Exception {
            mockMvc.perform(get("/api/batches"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    // =========================================================================
    // SECTION 5 : Validation des Endpoints PUBLICS
    // =========================================================================
    @Nested
    @DisplayName("Validation des Endpoints Publics (aucun header requis)")
    class PublicEndpointsTests {

        @Test
        @DisplayName("Le health check /api/health est accessible sans header ni authentification")
        void testHealthCheckIsPublic() throws Exception {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status", is("UP")));
        }

        @Test
        @DisplayName("La documentation OpenAPI /v3/api-docs est accessible sans authentification")
        void testOpenApiDocsIsPublic() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        @DisplayName("Les templates publics sont consultables par les deux entreprises et les visiteurs")
        void testPublicTemplatesAccessible() throws Exception {
            // Accessible pour Tenant A
            mockMvc.perform(get("/api/templates/" + templatePublic.getId())
                            .header("X-Entreprise-Code", TENANT_A))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nom", is("Certificat Standard Public")));

            // Accessible pour Tenant B
            mockMvc.perform(get("/api/templates/" + templatePublic.getId())
                            .header("X-Entreprise-Code", TENANT_B))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nom", is("Certificat Standard Public")));

            // Accessible sans header
            mockMvc.perform(get("/api/templates/" + templatePublic.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nom", is("Certificat Standard Public")));
        }

        @Test
        @DisplayName("Un endpoint privé sans authentification renvoie 401 ou 403")
        void testPrivateEndpointWithoutAuthReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/templates"))
                    .andExpect(status().is4xxClientError());
        }
    }
}
