package com.rapports.moteur.dto.dtoTemplate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Requête de mise à jour d'un modèle de rapport")
public record UpdateTemplateRequest(

        @Schema(description = "Nom du modèle", example = "Facture Client")
        @NotBlank
        String name,

        @Schema(description = "Description du modèle", example = "Facture standard pour clients professionnels")
        @NotBlank
        String description,

        @Schema(description = "Contenu du design au format JSON", example = "{\"pages\":[]}")
        @NotBlank
        String contenuDesign
) {
}