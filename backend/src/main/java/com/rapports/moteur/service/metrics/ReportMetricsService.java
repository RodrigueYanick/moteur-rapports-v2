package com.rapports.moteur.service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ReportMetricsService {

    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeBatchesGauge;

    public ReportMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.activeBatchesGauge = meterRegistry.gauge("reports.batches.active", new AtomicInteger(0));
    }

    /**
     * Enregistre les métriques d'une demande de génération de rapport.
     *
     * @param engine Moteur utilisé (gotenberg, flying-saucer, excel)
     * @param format Format généré (pdf, xlsx, factur-x)
     * @param status Statut du résultat (success, error)
     * @param tenant Code entreprise
     * @param duration Durée d'exécution
     */
    public void recordGeneration(String engine, String format, String status, String tenant, Duration duration) {
        String safeEngine = (engine != null && !engine.isBlank()) ? engine : "unknown";
        String safeFormat = (format != null && !format.isBlank()) ? format : "pdf";
        String safeStatus = (status != null && !status.isBlank()) ? status : "unknown";
        String safeTenant = (tenant != null && !tenant.isBlank()) ? tenant : "default";

        Counter.builder("reports.generation.requests")
                .description("Nombre total de générations de rapports demandées")
                .tag("engine", safeEngine)
                .tag("format", safeFormat)
                .tag("status", safeStatus)
                .tag("tenant", safeTenant)
                .register(meterRegistry)
                .increment();

        if (duration != null) {
            Timer.builder("reports.generation.duration")
                    .description("Durée de génération des rapports")
                    .tag("engine", safeEngine)
                    .tag("format", safeFormat)
                    .tag("status", safeStatus)
                    .register(meterRegistry)
                    .record(duration);
        }
    }

    /**
     * Met à jour le nombre de traitements par lot actuellement actifs.
     */
    public void setActiveBatchesCount(int count) {
        if (activeBatchesGauge != null) {
            activeBatchesGauge.set(Math.max(0, count));
        }
    }

    /**
     * Incrémente ou décrémente les lots actifs.
     */
    public void incrementActiveBatches() {
        if (activeBatchesGauge != null) {
            activeBatchesGauge.incrementAndGet();
        }
    }

    public void decrementActiveBatches() {
        if (activeBatchesGauge != null) {
            activeBatchesGauge.updateAndGet(c -> Math.max(0, c - 1));
        }
    }

    public int getActiveBatches() {
        return activeBatchesGauge != null ? activeBatchesGauge.get() : 0;
    }
}

