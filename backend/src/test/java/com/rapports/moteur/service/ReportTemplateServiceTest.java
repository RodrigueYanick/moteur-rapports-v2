package com.rapports.moteur.service;

import com.rapports.moteur.dto.dtoTemplate.TemplateRequest;
import com.rapports.moteur.dto.dtoTemplate.TemplateResponse;
import com.rapports.moteur.entity.TemplateStatus;
import com.rapports.moteur.repository.ReportTemplateRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

class ReportTemplateServiceTest {

    @Test
    void createShouldPersistDesignJsonAndDefaultStatus() {
        ReportTemplateRepository repository = Mockito.mock(ReportTemplateRepository.class);
        Mockito.when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReportTemplateService service = new ReportTemplateService(repository);
        TemplateRequest request = new TemplateRequest(
                "Facture",
                "Modèle de facture",
                "{\"blocs\":[{\"type\":\"titre\"}]}",
                List.of()
        );

        TemplateResponse response = service.create(request);

        assertEquals("Facture", response.name());
        assertEquals("{\"blocs\":[{\"type\":\"titre\"}]}", response.contenuDesign());
        assertEquals(TemplateStatus.ACTIVE, response.status());
    }
}
