package com.rapports.moteur.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.dtoTemplate.TemplateCreate;
import com.rapports.moteur.dto.dtoTemplate.TemplateResponse;
import com.rapports.moteur.entity.TemplateStatus;
import com.rapports.moteur.mapper.TemplateMapper;
import com.rapports.moteur.repository.ReportTemplateRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

class ReportTemplateServiceTest {

    @Test
    void createShouldPersistDesignJsonAndDefaultStatus() {
        ReportTemplateRepository repository = Mockito.mock(ReportTemplateRepository.class);
        TemplateMapper mapper = Mockito.mock(TemplateMapper.class);
        SchemaExtractorService schemaExtractorService = new SchemaExtractorService();
        ObjectMapper objectMapper = new ObjectMapper();

        // Prépare un TemplateCreate et les comportements du mapper/repository
        TemplateCreate request = new TemplateCreate();
        request.setNom("Facture");
        request.setDescription("Modèle de facture");
        request.setContenuDesign("{\"blocs\":[{\"type\":\"titre\"}]}");

        com.rapports.moteur.entity.ReportTemplate entity = new com.rapports.moteur.entity.ReportTemplate();
        entity.setNom(request.getNom());
        entity.setDescription(request.getDescription());
        entity.setContenuDesign(request.getContenuDesign());

        Mockito.when(mapper.toEntity(any())).thenReturn(entity);
        Mockito.when(repository.save(any())).thenAnswer(invocation -> Objects.requireNonNull(invocation.getArgument(0)));
        Mockito.when(mapper.toDto(any())).thenAnswer(invocation -> {
            com.rapports.moteur.entity.ReportTemplate e = invocation.getArgument(0);
            return TemplateResponse.builder()
                .nom(e.getNom())
                .description(e.getDescription())
                .contenuDesign(e.getContenuDesign())
                .statut(e.getStatut())
                .version(e.getVersion())
                .build();
        });

        ReportTemplateService service = new ReportTemplateService(repository, mapper, schemaExtractorService, objectMapper);

        TemplateResponse response = service.create(request);

        assertEquals("Facture", response.getNom());
        assertEquals("{\"blocs\":[{\"type\":\"titre\"}]}", response.getContenuDesign());
        assertEquals(TemplateStatus.BROUILLON, response.getStatut());
    }

    @Test
    void publishShouldExtractSchemaIncrementVersionAndSetPublie() {
        ReportTemplateRepository repository = Mockito.mock(ReportTemplateRepository.class);
        TemplateMapper mapper = Mockito.mock(TemplateMapper.class);
        SchemaExtractorService schemaExtractorService = new SchemaExtractorService();
        ObjectMapper objectMapper = new ObjectMapper();

        com.rapports.moteur.entity.ReportTemplate entity = new com.rapports.moteur.entity.ReportTemplate();
        entity.setId(java.util.UUID.randomUUID());
        entity.setNom("Facture");
        entity.setStatut(TemplateStatus.BROUILLON);
        entity.setVersion(1);
        entity.setContenuDesign("{\"blocs\":[{\"type\":\"titre\",\"contenu\":\"Rapport {{nom_client}}\"}]}");

        Mockito.when(repository.findById(entity.getId())).thenReturn(java.util.Optional.of(entity));
        Mockito.when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(mapper.toDto(any())).thenReturn(TemplateResponse.builder().nom("Facture").build());

        ReportTemplateService service = new ReportTemplateService(repository, mapper, schemaExtractorService, objectMapper);

        service.publish(entity.getId());

        assertEquals(TemplateStatus.PUBLIE, entity.getStatut());
        assertEquals(2, entity.getVersion());
        assertTrue(entity.getSchema().contains("nom_client"));
    }

    @Test
    void publishShouldRejectTemplateNotInBrouillon() {
        ReportTemplateRepository repository = Mockito.mock(ReportTemplateRepository.class);
        TemplateMapper mapper = Mockito.mock(TemplateMapper.class);
        SchemaExtractorService schemaExtractorService = new SchemaExtractorService();
        ObjectMapper objectMapper = new ObjectMapper();

        com.rapports.moteur.entity.ReportTemplate entity = new com.rapports.moteur.entity.ReportTemplate();
        entity.setId(java.util.UUID.randomUUID());
        entity.setStatut(TemplateStatus.PUBLIE);
        entity.setVersion(2);

        Mockito.when(repository.findById(entity.getId())).thenReturn(java.util.Optional.of(entity));

        ReportTemplateService service = new ReportTemplateService(repository, mapper, schemaExtractorService, objectMapper);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.rapports.moteur.exceptions.ValidationException.class,
                () -> service.publish(entity.getId()));
    }
}
