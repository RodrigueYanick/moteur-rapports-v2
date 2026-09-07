package com.rapports.moteur.dto.dtoTemplate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import com.rapports.moteur.entity.Categorie;
import com.rapports.moteur.entity.PaginationMode;

@Schema(description = "Requête de création ou de mise à jour d'un modèle de rapport")
@Data
public class TemplateCreate {

    @Schema(description = "Nom du modèle", example = "Facture Client")
    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @Schema(description = "Description optionnelle du modèle", example = "Modèle de facture standard pour les clients professionnels")
    private String description;

    @Schema(description = "Contenu du design au format JSON (généralement vide lors de la création)", example = "{\"pages\":[]}")
    private String contenuDesign;

    @Schema(description = "Catégorie du modèle", example = "VENTES", allowableValues = {
            "VENTES", "ACHATS", "FINANCE", "RH", "LOGISTIQUE", "STOCK", "PRODUCTION", "ADMINISTRATION", "AUTRES"})
    private Categorie categorie;

    @Schema(description = "Format papier du modèle", example = "A4", allowableValues = {"A4", "A5", "Letter", "Legal"})
    private String formatPapier;

    @Schema(description = "Largeur personnalisée en millimètres (obligatoire si formatPapier = CUSTOM)", example = "80")
    private Integer largeurMm;

    @Schema(description = "Hauteur personnalisée en millimètres (obligatoire si formatPapier = CUSTOM)", example = "200")
    private Integer hauteurMm;

    @Schema(description = "Mode de pagination du modèle", example = "FIXED", allowableValues = {"FIXED", "AUTO"})
    private PaginationMode modePagination = PaginationMode.FIXED;

    @Schema(description = "Marge gauche en mm", example = "5")
    private Integer margeGaucheMm;

    @Schema(description = "Marge droite en mm", example = "5")
    private Integer margeDroiteMm;

    @Schema(description = "Marge haute en mm", example = "5")
    private Integer margeHautMm;

    @Schema(description = "Marge basse en mm", example = "5")
    private Integer margeBasMm;
}