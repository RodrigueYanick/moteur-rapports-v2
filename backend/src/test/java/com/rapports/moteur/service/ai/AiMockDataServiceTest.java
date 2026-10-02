package com.rapports.moteur.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.dtoAi.AiMockDataRequest;
import com.rapports.moteur.dto.dtoAi.AiMockDataResponse;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.service.EntrepriseService;
import com.rapports.moteur.service.SchemaExtractorService;
import com.rapports.moteur.service.audit.AuditTrailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests Unitaires : AiMockDataService")
class AiMockDataServiceTest {

    @Mock
    private ReportTemplateRepository templateRepository;

    @Mock
    private SchemaExtractorService schemaExtractorService;

    @Mock
    private EntrepriseService entrepriseService;

    @Mock
    private AuditTrailService auditTrailService;

    @Mock
    private AiClientManager aiClientManager;

    private ObjectMapper objectMapper;
    private AiMockDataService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new AiMockDataService(
                templateRepository,
                schemaExtractorService,
                entrepriseService,
                auditTrailService,
                aiClientManager,
                objectMapper
        );
    }

    @Test
    @DisplayName("Génération réussie de mock data à partir d'une liste de variables")
    void testGenerateMockDataFromVariables() {
        AiMockDataRequest request = AiMockDataRequest.builder()
                .templateNom("Facture Test")
                .variables(List.of(
                        new AiMockDataRequest.VariableItem("client_nom", "STRING", "Nom du client"),
                        new AiMockDataRequest.VariableItem("total_ht", "FLOAT", "Total hors taxe")
                ))
                .build();

        String mockAiResponse = """
        {
          "client_nom": "Bâtiments & Frères SAS",
          "total_ht": 1500.00
        }
        """;

        when(aiClientManager.executeWithFallback(anyString(), anyString(), anyBoolean(), any(AtomicBoolean.class)))
                .thenReturn(mockAiResponse);
        when(aiClientManager.getCurrentProvider()).thenReturn("gemini");

        AiMockDataResponse response = service.generateMockData(request);

        assertThat(response).isNotNull();
        assertThat(response.getData()).containsEntry("client_nom", "Bâtiments & Frères SAS");
        assertThat(response.getData()).containsEntry("total_ht", 1500.00);
        assertThat(response.isFromAi()).isTrue();

        verify(auditTrailService).log(
                eq("AI_MOCK_DATA_GENERATE"),
                eq("MockData"),
                anyString(),
                anyString(),
                eq("SUCCESS")
        );
    }

    @Test
    @DisplayName("Génération avec repli heuristique si le retour IA n'est pas un JSON valide")
    void testFallbackWhenAiReturnsInvalidJson() {
        AiMockDataRequest request = AiMockDataRequest.builder()
                .templateNom("Devis")
                .variables(List.of(
                        new AiMockDataRequest.VariableItem("entreprise_siret", "STRING", null),
                        new AiMockDataRequest.VariableItem("date_emission", "DATE", null)
                ))
                .build();

        when(aiClientManager.executeWithFallback(anyString(), anyString(), anyBoolean(), any(AtomicBoolean.class)))
                .thenReturn("Ceci n'est pas du JSON");

        AiMockDataResponse response = service.generateMockData(request);

        assertThat(response).isNotNull();
        assertThat(response.isFromAi()).isFalse();
        assertThat(response.getData()).containsKey("entreprise_siret");
        assertThat(response.getData()).containsKey("date_emission");
    }

    @Test
    @DisplayName("Génération pour un templateId existant")
    void testGenerateMockDataFromTemplateId() {
        UUID templateId = UUID.randomUUID();
        ReportTemplate template = new ReportTemplate();
        template.setId(templateId);
        template.setNom("Devis Plomberie");
        template.setCodeEntreprise("ENT-001");
        template.setContenuDesign("{\"pages\":[]}");

        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn("ENT-001");
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(schemaExtractorService.extract(anyString())).thenReturn(List.of(
                new com.rapports.moteur.dto.dtoVariable.ExtractedVariable("nom_client", "STRING", true)
        ));
        when(aiClientManager.executeWithFallback(anyString(), anyString(), anyBoolean(), any(AtomicBoolean.class)))
                .thenReturn("{\"nom_client\":\"M. Martin\"}");
        when(aiClientManager.getCurrentProvider()).thenReturn("gemini");

        AiMockDataRequest request = AiMockDataRequest.builder()
                .templateId(templateId)
                .build();

        AiMockDataResponse response = service.generateMockData(request);

        assertThat(response).isNotNull();
        assertThat(response.getData()).containsEntry("nom_client", "M. Martin");
    }
}

