package com.rapports.moteur.dto.dtoGeneration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.UUID;

@Schema(description = "Réponse retournée lors d'une génération asynchrone")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GenerationResponse {

    @Schema(description = "Identifiant unique de la tâche de génération", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID generationId;

    @Schema(description = "Statut de la tâche de génération", example = "EN_COURS", allowableValues = {"EN_COURS", "SUCCES", "ECHEC"})
    private String status;
}