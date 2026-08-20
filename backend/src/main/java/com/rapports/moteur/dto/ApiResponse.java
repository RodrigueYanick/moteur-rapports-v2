package com.rapports.moteur.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Réponse standard de l'API")
public record ApiResponse<T>(
        @Schema(description = "Message décrivant le résultat de l'opération", example = "Opération réussie")
        String message,

        @Schema(description = "Données retournées, peut être null en cas d'erreur")
        T data,

        @Schema(description = "Horodatage de la réponse", example = "2026-08-19T14:30:00")
        String timestamp
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(message, data, LocalDateTime.now().toString());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(message, null, LocalDateTime.now().toString());
    }
}