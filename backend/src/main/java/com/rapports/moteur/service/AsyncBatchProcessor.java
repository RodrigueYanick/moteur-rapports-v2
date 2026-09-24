package com.rapports.moteur.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.dtoBatch.WebhookPayload;
import com.rapports.moteur.entity.*;
import com.rapports.moteur.repository.BatchGenerationItemRepository;
import com.rapports.moteur.repository.ReportBatchRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class AsyncBatchProcessor {

    private final ReportBatchRepository batchRepository;
    private final BatchGenerationItemRepository itemRepository;
    private final ReportGenerationService reportGenerationService;
    private final WebhookDeliveryService webhookDeliveryService;
    private final ObjectMapper objectMapper;
    private final com.rapports.moteur.service.metrics.ReportMetricsService reportMetricsService;

    public AsyncBatchProcessor(ReportBatchRepository batchRepository,
                               BatchGenerationItemRepository itemRepository,
                               ReportGenerationService reportGenerationService,
                               WebhookDeliveryService webhookDeliveryService,
                               com.rapports.moteur.service.metrics.ReportMetricsService reportMetricsService) {
        this.batchRepository = batchRepository;
        this.itemRepository = itemRepository;
        this.reportGenerationService = reportGenerationService;
        this.webhookDeliveryService = webhookDeliveryService;
        this.reportMetricsService = reportMetricsService;
        this.objectMapper = new ObjectMapper();
    }

    @Async("generationExecutor")
    public void processBatchAsync(@NonNull UUID batchId, boolean retryOnlyFailed) {
        log.info("Démarrage du traitement asynchrone du lot {} (retryOnlyFailed={})", batchId, retryOnlyFailed);
        reportMetricsService.incrementActiveBatches();

        ReportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalStateException("Lot introuvable : " + batchId));

        batch.setStatut(BatchStatus.EN_COURS);
        batchRepository.save(batch);

        List<BatchGenerationItem> itemsToProcess;
        if (retryOnlyFailed) {
            itemsToProcess = itemRepository.findByBatch_IdAndStatut(batchId, BatchItemStatus.ECHEC);
        } else {
            itemsToProcess = itemRepository.findByBatch_IdOrderByDateTraitementAsc(batchId);
        }

        int success = batch.getSuccessCount();
        int failures = batch.getFailureCount();

        for (BatchGenerationItem item : itemsToProcess) {
            item.setStatut(BatchItemStatus.EN_COURS);
            item.setDateTraitement(LocalDateTime.now());
            itemRepository.save(item);

            Map<String, Object> data = parseJson(item.getDonnees());

            try {
                ReportGeneration generation = reportGenerationService.generateAndStoreReport(batch.getTemplate(), data);
                item.setGeneration(generation);
                item.setUrlFichier(generation.getUrlFichierGenere());
                item.setStatut(BatchItemStatus.SUCCES);
                item.setErreur(null);
                success++;
            } catch (Exception e) {
                log.error("Échec lors de la génération de l'élément {} du lot {}", item.getId(), batchId, e);
                item.setStatut(BatchItemStatus.ECHEC);
                item.setErreur(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                failures++;
            }

            item.setDateTraitement(LocalDateTime.now());
            itemRepository.save(item);

            // Mise à jour de la progression globale en O(1)
            batch.setSuccessCount(success);
            batch.setFailureCount(failures);
            batch.setProcessedItems(success + failures);
            batchRepository.save(batch);
        }

        // Finalisation de l'état du lot
        finalizeBatch(batch, success, failures);
    }

    private void finalizeBatch(ReportBatch batch, int success, int failures) {
        UUID batchId = batch.getId();
        int total = batch.getTotalItems();

        batch.setSuccessCount(success);
        batch.setFailureCount(failures);
        batch.setProcessedItems(success + failures);
        batch.setDateFin(LocalDateTime.now());

        if (failures == 0 && success == total) {
            batch.setStatut(BatchStatus.TERMINE);
        } else if (success > 0) {
            batch.setStatut(BatchStatus.TERMINE_AVEC_ERREURS);
        } else {
            batch.setStatut(BatchStatus.ECHEC);
        }

        batchRepository.save(batch);
        reportMetricsService.decrementActiveBatches();
        log.info("Fin du traitement du lot {}. Statut={}, Succès={}, Échecs={}",
                batchId, batch.getStatut(), success, failures);

        // Déclenchement du Webhook s'il est configuré
        triggerWebhookNotification(batch);
    }

    private void triggerWebhookNotification(ReportBatch batch) {
        String webhookUrl = batch.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        log.info("Envoi de la notification Webhook pour le lot {} vers {}", batch.getId(), webhookUrl);

        List<BatchGenerationItem> allItems = itemRepository.findByBatch_IdOrderByDateTraitementAsc(batch.getId());
        List<WebhookPayload.BatchItemSummary> summaries = allItems.stream()
                .map(item -> WebhookPayload.BatchItemSummary.builder()
                        .customId(item.getCustomId())
                        .statut(item.getStatut().name())
                        .erreur(item.getErreur())
                        .downloadUrl(item.getGeneration() != null
                                ? "/api/templates/generations/" + item.getGeneration().getId() + "/download"
                                : null)
                        .build())
                .toList();

        String eventName = (batch.getStatut() == BatchStatus.TERMINE) ? "batch.completed" : "batch.failed";

        WebhookPayload payload = WebhookPayload.builder()
                .event(eventName)
                .timestamp(LocalDateTime.now())
                .batchId(batch.getId())
                .templateId(batch.getTemplate().getId())
                .templateNom(batch.getTemplate().getNom())
                .statut(batch.getStatut())
                .totalItems(batch.getTotalItems())
                .successCount(batch.getSuccessCount())
                .failureCount(batch.getFailureCount())
                .downloadZipUrl("/api/batches/" + batch.getId() + "/download-zip")
                .items(summaries)
                .build();

        boolean delivered = webhookDeliveryService.sendWebhook(
                webhookUrl,
                batch.getWebhookSecret(),
                eventName,
                payload
        );

        batch.setWebhookStatut(delivered ? WebhookStatus.ENVOYE : WebhookStatus.ECHEC);
        batch.setWebhookTentatives(batch.getWebhookTentatives() + 1);
        batchRepository.save(batch);
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Impossible de parser le JSON des données d'un item : {}", json, e);
            return Collections.emptyMap();
        }
    }
}

