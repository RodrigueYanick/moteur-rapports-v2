package com.rapports.moteur.service.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ReportMetricsServiceTest {

    private MeterRegistry meterRegistry;
    private ReportMetricsService metricsService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new ReportMetricsService(meterRegistry);
    }

    @Test
    @DisplayName("recordGeneration incrémente le compteur et enregistre la durée")
    void testRecordGeneration() {
        metricsService.recordGeneration("gotenberg", "pdf", "success", "ENT_ALPHA", Duration.ofMillis(350));

        double count = meterRegistry.get("reports.generation.requests")
                .tag("engine", "gotenberg")
                .tag("format", "pdf")
                .tag("status", "success")
                .tag("tenant", "ENT_ALPHA")
                .counter()
                .count();
        assertThat(count).isEqualTo(1.0);

        double totalTime = meterRegistry.get("reports.generation.duration")
                .tag("engine", "gotenberg")
                .tag("format", "pdf")
                .tag("status", "success")
                .timer()
                .totalTime(java.util.concurrent.TimeUnit.MILLISECONDS);
        assertThat(totalTime).isGreaterThanOrEqualTo(350.0);
    }

    @Test
    @DisplayName("Incrémentation et décrémentation de la jauge activeBatches")
    void testActiveBatchesGauge() {
        assertThat(metricsService.getActiveBatches()).isEqualTo(0);

        metricsService.incrementActiveBatches();
        metricsService.incrementActiveBatches();
        assertThat(metricsService.getActiveBatches()).isEqualTo(2);

        metricsService.decrementActiveBatches();
        assertThat(metricsService.getActiveBatches()).isEqualTo(1);

        metricsService.decrementActiveBatches();
        metricsService.decrementActiveBatches(); // ne doit pas être négatif
        assertThat(metricsService.getActiveBatches()).isEqualTo(0);
    }
}
