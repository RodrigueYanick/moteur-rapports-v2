package com.rapports.moteur.dto.dtoBatch;

import com.rapports.moteur.entity.BatchStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookPayload {

    private String event;
    private LocalDateTime timestamp;
    private UUID batchId;
    private UUID templateId;
    private String templateNom;
    private BatchStatus statut;
    private int totalItems;
    private int successCount;
    private int failureCount;
    private String downloadZipUrl;
    private List<BatchItemSummary> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchItemSummary {
        private String customId;
        private String statut;
        private String erreur;
        private String downloadUrl;
    }
}

