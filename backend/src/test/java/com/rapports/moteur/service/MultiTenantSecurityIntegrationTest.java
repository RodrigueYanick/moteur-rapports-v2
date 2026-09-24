package com.rapports.moteur.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.dtoDocument.DocumentCreate;
import com.rapports.moteur.dto.dtoDocument.DocumentResponse;
import com.rapports.moteur.entity.*;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.repository.DocumentRepository;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.security.UrlSecurityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Sprint 1 - Sécurité Multi-Tenant & Protection SSRF")
class MultiTenantSecurityIntegrationTest {

    @Nested
    @DisplayName("Isolation Multi-Tenant des Documents")
    class MultiTenantDocumentIsolationTests {

        @Mock
        private DocumentRepository documentRepository;

        @Mock
        private ReportTemplateRepository templateRepository;

        @Spy
        private ObjectMapper objectMapper = new ObjectMapper();

        @Mock
        private EntrepriseService entrepriseService;

        @InjectMocks
        private DocumentService documentService;

        private UUID templateId;
        private UUID docTenantAId;
        private ReportTemplate sharedTemplate;
        private Document docTenantA;

        @BeforeEach
        void setUp() {
            templateId = UUID.randomUUID();
            docTenantAId = UUID.randomUUID();

            // Modèle public / partagé
            sharedTemplate = ReportTemplate.builder()
                    .id(templateId)
                    .nom("Modèle Facture")
                    .codeEntreprise(null) // Public
                    .build();

            // Document créé par le Tenant A (ALPHA)
            docTenantA = Document.builder()
                    .id(docTenantAId)
                    .nom("Facture_ALPHA_001")
                    .template(sharedTemplate)
                    .codeEntreprise("ENT_ALPHA")
                    .dateCreation(LocalDateTime.now())
                    .build();
        }

        @Test
        @DisplayName("Le Tenant B ne doit pas voir les documents du Tenant A même sur un template public")
        void testTenantBCannotSeeTenantADocuments() {
            // Contexte : Tenant B est connecté
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(sharedTemplate));
            when(entrepriseService.getCurrentCodeEntreprise()).thenReturn("ENT_BETA");
            when(documentRepository.findByTemplateIdAndCodeEntreprise(templateId, "ENT_BETA"))
                    .thenReturn(List.of());

            List<DocumentResponse> result = documentService.getByTemplate(templateId);

            assertThat(result).isEmpty();
            verify(documentRepository).findByTemplateIdAndCodeEntreprise(templateId, "ENT_BETA");
            verify(documentRepository, never()).findAll();
        }

        @Test
        @DisplayName("Le Tenant B est rejeté lorsqu'il tente d'accéder directement au document du Tenant A")
        void testTenantBCannotAccessTenantADocumentById() {
            // Contexte : Tenant B tente d'accéder au document docTenantAId
            when(documentRepository.findById(docTenantAId)).thenReturn(Optional.of(docTenantA));
            when(entrepriseService.getCurrentCodeEntreprise()).thenReturn("ENT_BETA");

            assertThatThrownBy(() -> documentService.getById(templateId, docTenantAId))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Document introuvable");
        }

        @Test
        @DisplayName("Le Tenant B est rejeté lorsqu'il tente de supprimer le document du Tenant A")
        void testTenantBCannotDeleteTenantADocument() {
            when(documentRepository.findById(docTenantAId)).thenReturn(Optional.of(docTenantA));
            when(entrepriseService.getCurrentCodeEntreprise()).thenReturn("ENT_BETA");

            assertThatThrownBy(() -> documentService.delete(templateId, docTenantAId))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Document introuvable");

            verify(documentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("La création d'un document associe obligatoirement le code de l'entreprise connectée")
        void testCreateDocumentEnforcesCurrentTenantCode() {
            when(entrepriseService.getCurrentCodeEntreprise()).thenReturn("ENT_ALPHA");
            when(templateRepository.findById(templateId)).thenReturn(Optional.of(sharedTemplate));
            when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.setId(UUID.randomUUID());
                return doc;
            });

            DocumentCreate request = new DocumentCreate();
            request.setNom("AlphaDoc");
            request.setDonnees(Map.of("client", "Acme Corp"));

            DocumentResponse response = documentService.create(templateId, request);

            assertThat(response).isNotNull();
            assertThat(response.getCodeEntreprise()).isEqualTo("ENT_ALPHA");
            verify(documentRepository).save(argThat(doc -> "ENT_ALPHA".equals(doc.getCodeEntreprise())));
        }
    }

    @Nested
    @DisplayName("Protection SSRF (Server-Side Request Forgery)")
    class SsrfProtectionTests {

        private final UrlSecurityValidator validator = new UrlSecurityValidator();

        @Test
        @DisplayName("Les URLs publiques légitimes HTTPS et HTTP sont acceptées")
        void testValidPublicUrlsAccepted() {
            validator.validateSafeUrl("https://8.8.8.8/webhook");
            validator.validateSafeUrl("https://1.1.1.1/api");
        }

        @Test
        @DisplayName("Les protocoles non-HTTP (file, ftp, gopher) sont rejetés")
        void testDisallowedProtocolsRejected() {
            assertThatThrownBy(() -> validator.validateSafeUrl("ftp://example.com/upload"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Protocole non autorisé");

            assertThatThrownBy(() -> validator.validateSafeUrl("file:///etc/passwd"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Protocole non autorisé");

            assertThatThrownBy(() -> validator.validateSafeUrl("gopher://127.0.0.1:70/"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("Protocole non autorisé");
        }

        @Test
        @DisplayName("Les boucles locales (localhost, 127.0.0.1) sont bloquées")
        void testLoopbackAddressesBlocked() {
            assertThatThrownBy(() -> validator.validateSafeUrl("http://127.0.0.1:8080/internal"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("SSRF");

            assertThatThrownBy(() -> validator.validateSafeUrl("http://localhost:5432/admin"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("SSRF");
        }

        @Test
        @DisplayName("Les plages IP privées RFC 1918 (10.x, 172.16.x, 192.168.x) sont bloquées")
        void testPrivateIpRangesBlocked() {
            assertThatThrownBy(() -> validator.validateSafeUrl("http://10.0.0.5/api"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("SSRF");

            assertThatThrownBy(() -> validator.validateSafeUrl("http://172.16.10.20/service"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("SSRF");

            assertThatThrownBy(() -> validator.validateSafeUrl("http://192.168.1.1:80/admin"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("SSRF");
        }

        @Test
        @DisplayName("L'IP de métadonnées Cloud AWS/GCP/Azure (169.254.169.254) est bloquée")
        void testCloudMetadataBlocked() {
            assertThatThrownBy(() -> validator.validateSafeUrl("http://169.254.169.254/latest/meta-data/"))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("SSRF");
        }

        @Test
        @DisplayName("pingWebhook neutralise immédiatement une URL malveillante avec statut false")
        void testPingWebhookBlocksMaliciousUrl() {
            WebhookDeliveryService webhookService = new WebhookDeliveryService(validator);

            var response = webhookService.pingWebhook("http://169.254.169.254/latest/meta-data/", "test-secret");

            assertThat(response.isSucces()).isFalse();
            assertThat(response.getMessage()).contains("SSRF");
        }

        @Test
        @DisplayName("sendWebhook renvoie false sans tenter d'appel HTTP vers une adresse locale")
        void testSendWebhookReturnsFalseOnSsrf() {
            WebhookDeliveryService webhookService = new WebhookDeliveryService(validator);

            boolean result = webhookService.sendWebhook("http://127.0.0.1:5432/exploit", "secret", "batch.completed", "{}");

            assertThat(result).isFalse();
        }
    }
}

