package com.rapports.moteur.integration;

import com.rapports.moteur.dto.dtoBatch.BatchCreateRequest;
import com.rapports.moteur.dto.dtoBatch.BatchItemRequest;
import com.rapports.moteur.dto.dtoBatch.WebhookTestRequest;
import com.rapports.moteur.dto.dtoDocument.DocumentCreate;
import com.rapports.moteur.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Tests d'Intégration : Gestion des Documents, Générations par Lot (Batch) et Webhooks")
class DocumentAndBatchIntegrationTest extends BaseIntegrationTest {

    private Entreprise testEntreprise;
    private User testUser;
    private String token;
    private ReportTemplate testTemplate;

    @BeforeEach
    void initContext() {
        testEntreprise = createEntreprise("LOGIX_CORP", "Logix Corporation");
        testUser = createUser("operator@logix.com", "Password123!", Role.OPERATOR, testEntreprise);
        token = getBearerToken(testUser);

        testTemplate = createTemplate("Bon de Préparation", "LOGIX_CORP", TemplateStatus.PUBLIE);
    }

    @Nested
    @DisplayName("1. Cas Nominaux Documents (Happy Path)")
    class DocumentHappyPathTests {

        @Test
        @DisplayName("Cycle complet d'un Document : création, lecture, mise à jour et suppression")
        void shouldHandleFullDocumentCrudLifecycle() throws Exception {
            // 1. Création
            DocumentCreate createReq = new DocumentCreate();
            createReq.setNom("Commande_LOGIX_1001");
            createReq.setDonnees("{\"articles\": [\"Scanner\", \"Imprimante\"], \"total\": 850}");

            String createResponse = mockMvc.perform(post("/api/templates/" + testTemplate.getId() + "/documents")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.nom").value("Commande_LOGIX_1001"))
                    .andExpect(jsonPath("$.statut").value("BROUILLON"))
                    .andReturn().getResponse().getContentAsString();

            UUID docId = UUID.fromString(objectMapper.readTree(createResponse).get("id").asText());

            // Vérification persistance réelle
            Document persisted = documentRepository.findById(docId).orElseThrow();
            assertThat(persisted.getNom()).isEqualTo("Commande_LOGIX_1001");
            assertThat(persisted.getCodeEntreprise()).isEqualTo("LOGIX_CORP");

            // 2. Consultation unitaire
            mockMvc.perform(get("/api/templates/" + testTemplate.getId() + "/documents/" + docId)
                            .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(docId.toString()));

            // 3. Mise à jour
            createReq.setNom("Commande_LOGIX_1001_V2");
            createReq.setDonnees("{\"articles\": [\"Scanner\"], \"total\": 450}");
            mockMvc.perform(put("/api/templates/" + testTemplate.getId() + "/documents/" + docId)
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nom").value("Commande_LOGIX_1001_V2"));

            // 4. Consultation globale (/api/documents)
            mockMvc.perform(get("/api/documents")
                            .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));

            // 5. Suppression
            mockMvc.perform(delete("/api/templates/" + testTemplate.getId() + "/documents/" + docId)
                            .header("Authorization", token))
                    .andExpect(status().isNoContent());

            assertThat(documentRepository.findById(docId)).isEmpty();
        }
    }

    @Nested
    @DisplayName("2. Cas Nominaux Traitement par Lot (Batch Happy Path)")
    class BatchHappyPathTests {

        @Test
        @DisplayName("Lancement d'un lot de génération avec éléments et persistance en base")
        void shouldCreateBatchWithItemsSuccessfully() throws Exception {
            BatchCreateRequest batchRequest = BatchCreateRequest.builder()
                    .templateId(testTemplate.getId())
                    .webhookUrl("https://safe-company-endpoint.com/webhook")
                    .webhookSecret("secretKey123")
                    .items(List.of(
                            BatchItemRequest.builder()
                                    .customId("ITEM-001")
                                    .data(Map.of("client", "Société A", "montant", 100))
                                    .build(),
                            BatchItemRequest.builder()
                                    .customId("ITEM-002")
                                    .data(Map.of("client", "Société B", "montant", 200))
                                    .build()
                    ))
                    .build();

            String batchResponse = mockMvc.perform(post("/api/batches")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(batchRequest)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.id").isNotEmpty())
                    .andExpect(jsonPath("$.totalItems").value(2))
                    .andReturn().getResponse().getContentAsString();

            UUID batchId = UUID.fromString(objectMapper.readTree(batchResponse).get("id").asText());

            // Vérification de la persistance en base
            ReportBatch batch = batchRepository.findById(batchId).orElseThrow();
            assertThat(batch.getTotalItems()).isEqualTo(2);
            assertThat(batch.getCodeEntreprise()).isEqualTo("LOGIX_CORP");

            List<BatchGenerationItem> items = batchItemRepository.findByBatch_IdOrderByDateTraitementAsc(batchId);
            assertThat(items).hasSize(2);

            // Consultation de l'état du lot
            mockMvc.perform(get("/api/batches/" + batchId)
                            .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(batchId.toString()))
                    .andExpect(jsonPath("$.totalItems").value(2));

            // Consultation des éléments du lot
            mockMvc.perform(get("/api/batches/" + batchId + "/items")
                            .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)));
        }
    }

    @Nested
    @DisplayName("3. Sécurité SSRF & Intégration Externe Webhooks")
    class WebhookSecurityAndExternalIntegrationTests {

        @Test
        @DisplayName("Blocage SSRF : Rejet des URLs locales ou privées lors du ping de webhook")
        void shouldBlockSsrfWebhookTargets() throws Exception {
            // Tentative vers boucle locale
            WebhookTestRequest loopbackReq = WebhookTestRequest.builder()
                    .url("http://127.0.0.1:8080/exploit")
                    .secret("test")
                    .build();

            mockMvc.perform(post("/api/batches/webhooks/test")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loopbackReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.succes").value(false))
                    .andExpect(jsonPath("$.message", containsString("SSRF")));

            // Tentative vers métadonnées Cloud AWS/GCP
            WebhookTestRequest cloudMetaReq = WebhookTestRequest.builder()
                    .url("http://169.254.169.254/latest/meta-data")
                    .secret("test")
                    .build();

            mockMvc.perform(post("/api/batches/webhooks/test")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cloudMetaReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.succes").value(false))
                    .andExpect(jsonPath("$.message", containsString("SSRF")));
        }

        @Test
        @DisplayName("Tolérance aux pannes : Gestion propre d'une URL de webhook externe inaccessible")
        void shouldHandleUnreachableExternalWebhookGracefully() throws Exception {
            WebhookTestRequest unreachableReq = WebhookTestRequest.builder()
                    .url("https://unreachable-mock-webhook-target-999.xyz/hook")
                    .secret("test")
                    .build();

            mockMvc.perform(post("/api/batches/webhooks/test")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(unreachableReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.succes").value(false));
        }
    }

    @Nested
    @DisplayName("4. Cas aux Limites & Validation")
    class EdgeCasesTests {

        @Test
        @DisplayName("Rejet : création de document avec un nom vide (400 Bad Request)")
        void shouldRejectDocumentWithEmptyName() throws Exception {
            DocumentCreate emptyName = new DocumentCreate();
            emptyName.setNom("");

            mockMvc.perform(post("/api/templates/" + testTemplate.getId() + "/documents")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emptyName)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }

        @Test
        @DisplayName("Rejet : création de lot avec une liste d'éléments vide (400 Bad Request)")
        void shouldRejectBatchWithEmptyItems() throws Exception {
            BatchCreateRequest emptyBatch = BatchCreateRequest.builder()
                    .templateId(testTemplate.getId())
                    .items(List.of()) // vide
                    .build();

            mockMvc.perform(post("/api/batches")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(emptyBatch)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message", containsString("ne peut pas être vide")));
        }

        @Test
        @DisplayName("Rejet : document sur un modèle inexistant (404 Not Found)")
        void shouldReturn404WhenCreatingDocumentOnUnknownTemplate() throws Exception {
            DocumentCreate doc = new DocumentCreate();
            doc.setNom("Doc Test");
            doc.setDonnees(Map.of());

            mockMvc.perform(post("/api/templates/" + UUID.randomUUID() + "/documents")
                            .header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(doc)))
                    .andExpect(status().isNotFound());
        }
    }
}
