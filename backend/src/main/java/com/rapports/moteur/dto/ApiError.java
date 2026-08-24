package com.rapports.moteur.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "Réponse standard en cas d'erreur")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ApiError {

    @Schema(description = "Horodatage de l'erreur", example = "2026-08-21T14:30:00")
    private LocalDateTime timestamp;

    @Schema(description = "Code HTTP de l'erreur", example = "404")
    private int status;

    @Schema(description = "Type d'erreur", example = "Not Found")
    private String error;

    @Schema(description = "Message détaillé de l'erreur", example = "Template introuvable : 3f071462-...")
    private String message;

    @Schema(description = "Chemin de la requête", example = "/api/templates/3f071462-...")
    private String path;
}