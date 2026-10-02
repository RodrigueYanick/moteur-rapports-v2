package com.rapports.moteur.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.dtoAi.AiTemplateGenerationResponse;
import com.rapports.moteur.dto.dtoAi.AiTemplatePromptRequest;
import com.rapports.moteur.dto.dtoVariable.ExtractedVariable;
import com.rapports.moteur.entity.*;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.repository.ReportVariableRepository;
import com.rapports.moteur.service.CompanyWorkspaceConfigService;
import com.rapports.moteur.service.EntrepriseService;
import com.rapports.moteur.service.SchemaExtractorService;
import com.rapports.moteur.service.audit.AuditTrailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests Unitaires : AiTemplateBuilderService")
class AiTemplateBuilderServiceTest {

    @Mock
    private ReportTemplateRepository templateRepository;

    @Mock
    private ReportVariableRepository variableRepository;

    @Mock
    private SchemaExtractorService schemaExtractorService;

    @Mock
    private EntrepriseService entrepriseService;

    @Mock
    private CompanyWorkspaceConfigService workspaceConfigService;

    @Mock
    private AuditTrailService auditTrailService;

    @Mock
    private AiClientManager aiClientManager;

    private ObjectMapper objectMapper;
    private AiTemplateBuilderService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new AiTemplateBuilderService(
                templateRepository,
                variableRepository,
                schemaExtractorService,
                entrepriseService,
                workspaceConfigService,
                auditTrailService,
                aiClientManager,
                objectMapper
        );
    }

    @Test
    @DisplayName("Génération réussie d'un modèle avec extraction des variables et audit")
    void testGenerateTemplateSuccess() {
        AiTemplatePromptRequest request = AiTemplatePromptRequest.builder()
                .prompt("Génère un devis artisan avec logo et tableau de prestations")
                .nom("Devis Artisan Pro")
                .categorie("VENTES")
                .build();

        String generatedJson = """
        {
          "nom": "Devis Artisan Pro",
          "description": "Modèle généré par IA",
          "categorie": "VENTES",
          "formatPapier": "A4",
          "pages": [
            {
              "nom": "Page 1",
              "blocs": [
                { "id": "b1", "type": "titre", "contenu": "DEVIS {{numero_devis}}" },
                { "id": "b2", "type": "texte", "contenu": "Client: {{nom_client}}" }
              ]
            }
          ]
        }
        """;

        when(aiClientManager.executeWithFallback(anyString(), anyString(), anyBoolean(), any(AtomicBoolean.class)))
                .thenReturn(generatedJson);
        when(aiClientManager.getCurrentProvider()).thenReturn("gemini");
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn("ENT-001");
        when(workspaceConfigService.getEntityForCurrentEntreprise()).thenReturn(new CompanyWorkspaceConfig());

        UUID templateId = UUID.randomUUID();
        when(templateRepository.save(any(ReportTemplate.class))).thenAnswer(inv -> {
            ReportTemplate t = inv.getArgument(0);
            t.setId(templateId);
            return t;
        });

        when(schemaExtractorService.extract(anyString())).thenReturn(List.of(
                new ExtractedVariable("numero_devis", "STRING", true),
                new ExtractedVariable("nom_client", "STRING", true)
        ));

        AiTemplateGenerationResponse response = service.generateTemplate(request);

        assertThat(response).isNotNull();
        assertThat(response.getTemplateId()).isEqualTo(templateId);
        assertThat(response.getNom()).isEqualTo("Devis Artisan Pro");
        assertThat(response.getCategorie()).isEqualTo("VENTES");
        assertThat(response.getExtractedVariables()).containsExactlyInAnyOrder("numero_devis", "nom_client");

        // Vérifier que le template a été persisté avec le statut BROUILLON et version 1
        ArgumentCaptor<ReportTemplate> templateCaptor = ArgumentCaptor.forClass(ReportTemplate.class);
        verify(templateRepository).save(templateCaptor.capture());
        ReportTemplate captured = templateCaptor.getValue();
        assertThat(captured.getStatut()).isEqualTo(TemplateStatus.BROUILLON);
        assertThat(captured.getVersion()).isEqualTo(1);
        assertThat(captured.getCodeEntreprise()).isEqualTo("ENT-001");

        // Vérifier la persistance des variables
        verify(variableRepository).saveAll(anyList());

        // Vérifier la journalisation dans la piste d'audit
        verify(auditTrailService).log(
                eq("AI_TEMPLATE_GENERATE"),
                eq("ReportTemplate"),
                eq(templateId.toString()),
                anyString(),
                eq("SUCCESS")
        );
    }

    @Test
    @DisplayName("Un prompt vide lève une exception IllegalArgumentException")
    void testEmptyPromptThrows() {
        AiTemplatePromptRequest request = AiTemplatePromptRequest.builder()
                .prompt("   ")
                .build();

        assertThatThrownBy(() -> service.generateTemplate(request))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

