package com.rapports.moteur.dto.dtoBatch;

import com.rapports.moteur.entity.BatchItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Détail de l'état d'un élément d'un lot")
public class BatchItemResponse {

    private UUID id;
    private UUID batchId;
    private UUID generationId;
    private String customId;
    private BatchItemStatus statut;
    private String urlFichier;
    private String downloadUrl;
    private String erreur;
    private LocalDateTime dateTraitement;
}

