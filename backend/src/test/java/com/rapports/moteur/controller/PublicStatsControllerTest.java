package com.rapports.moteur.controller;

import com.rapports.moteur.dto.dtoPublic.PublicStatsDto;
import com.rapports.moteur.repository.DocumentRepository;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicStatsControllerTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReportTemplateRepository templateRepository;

    private PublicStatsController statsController;

    @BeforeEach
    void setUp() {
        statsController = new PublicStatsController(documentRepository, userRepository, templateRepository);
    }

    @Test
    @DisplayName("GET /api/public/stats : agrège les métriques réelles avec le socle minimum de confiance")
    void testGetPublicStats_ReturnsAggregatedMetrics() {
        when(documentRepository.count()).thenReturn(150L);
        when(userRepository.count()).thenReturn(42L);
        when(templateRepository.count()).thenReturn(8L);

        ResponseEntity<PublicStatsDto> response = statsController.getPublicStats();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        PublicStatsDto stats = response.getBody();
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalDocuments()).isEqualTo(250_150L);
        assertThat(stats.getTotalUsers()).isEqualTo(45_042L);
        assertThat(stats.getTotalTemplates()).isEqualTo(128L);
        assertThat(stats.getSatisfactionRate()).isGreaterThan(99.0);
        assertThat(stats.getActiveCountries()).isGreaterThan(30);
    }
}

