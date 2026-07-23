package com.rapports.moteur.service;

//import com.rapports.moteur.dto.dtoTemplate.TemplateRequest;
import com.rapports.moteur.dto.dtoTemplate.TemplateCreate;
import com.rapports.moteur.dto.dtoTemplate.TemplateResponse;
import com.rapports.moteur.entity.TemplateStatus;
import com.rapports.moteur.mapper.TemplateMapper;
import com.rapports.moteur.mapper.VariableMapper;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.repository.ReportVariableRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

class ReportTemplateServiceTest {

    @Test
    void createShouldPersistDesignJsonAndDefaultStatus() {
        ReportTemplateRepository repository = Mockito.mock(ReportTemplateRepository.class);
        VariableMapper variableMapper = Mockito.mock(VariableMapper.class);
        ReportVariableRepository variableRepository = Mockito.mock(ReportVariableRepository.class);
        TemplateMapper mapper = Mockito.mock(TemplateMapper.class);

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

        ReportTemplateService service = new ReportTemplateService(variableMapper, repository, variableRepository, mapper);

        TemplateResponse response = service.create(request);

        assertEquals("Facture", response.getNom());
        assertEquals("{\"blocs\":[{\"type\":\"titre\"}]}", response.getContenuDesign());
        assertEquals(TemplateStatus.BROUILLON, response.getStatut());
    }
}
