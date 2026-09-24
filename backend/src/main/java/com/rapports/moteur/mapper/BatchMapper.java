package com.rapports.moteur.mapper;

import com.rapports.moteur.dto.dtoBatch.BatchItemResponse;
import com.rapports.moteur.dto.dtoBatch.BatchResponse;
import com.rapports.moteur.entity.BatchGenerationItem;
import com.rapports.moteur.entity.ReportBatch;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Component
public class BatchMapper {

    public BatchResponse toResponse(ReportBatch entity, boolean includeItems) {
        if (entity == null) return null;

        double progression = entity.getTotalItems() > 0
                ? Math.round(((double) entity.getProcessedItems() / entity.getTotalItems()) * 1000.0) / 10.0
                : 0.0;

        Long dureeSecondes = null;
        if (entity.getDateCreation() != null && entity.getDateFin() != null) {
            dureeSecondes = Duration.between(entity.getDateCreation(), entity.getDateFin()).getSeconds();
        }

        List<BatchItemResponse> items = Collections.emptyList();
        if (includeItems && entity.getItems() != null) {
            items = entity.getItems().stream()
                    .map(this::toItemResponse)
                    .toList();
        }

        return BatchResponse.builder()
                .id(entity.getId())
                .templateId(entity.getTemplate() != null ? entity.getTemplate().getId() : null)
                .templateNom(entity.getTemplate() != null ? entity.getTemplate().getNom() : null)
                .codeEntreprise(entity.getCodeEntreprise())
                .statut(entity.getStatut())
                .totalItems(entity.getTotalItems())
                .processedItems(entity.getProcessedItems())
                .successCount(entity.getSuccessCount())
                .failureCount(entity.getFailureCount())
                .progressionPourcentage(progression)
                .webhookUrl(entity.getWebhookUrl())
                .webhookStatut(entity.getWebhookStatut())
                .webhookTentatives(entity.getWebhookTentatives())
                .erreur(entity.getErreur())
                .dateCreation(entity.getDateCreation())
                .dateFin(entity.getDateFin())
                .dureeSecondes(dureeSecondes)
                .items(items)
                .build();
    }

    public BatchItemResponse toItemResponse(BatchGenerationItem item) {
        if (item == null) return null;

        String downloadUrl = null;
        if (item.getGeneration() != null) {
            downloadUrl = "/api/templates/generations/" + item.getGeneration().getId() + "/download";
        }

        return BatchItemResponse.builder()
                .id(item.getId())
                .batchId(item.getBatch() != null ? item.getBatch().getId() : null)
                .generationId(item.getGeneration() != null ? item.getGeneration().getId() : null)
                .customId(item.getCustomId())
                .statut(item.getStatut())
                .urlFichier(item.getUrlFichier())
                .downloadUrl(downloadUrl)
                .erreur(item.getErreur())
                .dateTraitement(item.getDateTraitement())
                .build();
    }
}

