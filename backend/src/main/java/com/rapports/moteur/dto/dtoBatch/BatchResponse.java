package com.rapports.moteur.dto.dtoBatch;

import com.rapports.moteur.entity.BatchStatus;
import com.rapports.moteur.entity.WebhookStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "État global et progression d'un lot de génération")
public class BatchResponse {

    private UUID id;
    private UUID templateId;
    private String templateNom;
    private String codeEntreprise;
    private BatchStatus statut;
    private int totalItems;
    private int processedItems;
    private int successCount;
    private int failureCount;
    private double progressionPourcentage;
    private String webhookUrl;
    private WebhookStatus webhookStatut;
    private int webhookTentatives;
    private String erreur;
    private LocalDateTime dateCreation;
    private LocalDateTime dateFin;
    private Long dureeSecondes;
    private List<BatchItemResponse> items;
}

