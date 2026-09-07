package com.rapports.moteur.dto.dtoTemplate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import com.rapports.moteur.entity.PaginationMode;

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
        String contenuDesign,

        @Schema(description = "Format papier du modèle", example = "A4")
        String formatPapier,

        @Schema(description = "Largeur personnalisée en mm", example = "80")
        Integer largeurMm,

        @Schema(description = "Hauteur personnalisée en mm", example = "200")
        Integer hauteurMm,

        @Schema(description = "Mode de pagination", example = "FIXED")
        PaginationMode modePagination,

        @Schema(description = "Marge gauche en mm", example = "5")
        Integer margeGaucheMm,

        @Schema(description = "Marge droite en mm", example = "5")
        Integer margeDroiteMm,

        @Schema(description = "Marge haute en mm", example = "5")
        Integer margeHautMm,

        @Schema(description = "Marge basse en mm", example = "5")
        Integer margeBasMm
) {
}