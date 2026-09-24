package com.rapports.moteur.dto.dtoWorkspace;

import com.rapports.moteur.entity.PaginationMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

@Schema(description = "Requête de configuration de la feuille de travail pour une entreprise")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceConfigRequest {

    @Schema(description = "Format papier", example = "A4")
    private String formatPapier;

    @Schema(description = "Largeur en mm (si CUSTOM)", example = "210")
    @Min(value = 10, message = "La largeur doit être d'au moins 10mm")
    private Integer largeurMm;

    @Schema(description = "Hauteur en mm (si CUSTOM)", example = "297")
    @Min(value = 10, message = "La hauteur doit être d'au moins 10mm")
    private Integer hauteurMm;

    @Schema(description = "Mode de pagination par défaut", example = "FIXED")
    private PaginationMode modePagination;

    @Schema(description = "Marge gauche en mm", example = "10")
    @Min(value = 0, message = "La marge gauche ne peut pas être négative")
    @Max(value = 100, message = "La marge gauche ne peut pas dépasser 100mm")
    private Integer margeGaucheMm;

    @Schema(description = "Marge droite en mm", example = "10")
    @Min(value = 0, message = "La marge droite ne peut pas être négative")
    @Max(value = 100, message = "La marge droite ne peut pas dépasser 100mm")
    private Integer margeDroiteMm;

    @Schema(description = "Marge haute en mm", example = "10")
    @Min(value = 0, message = "La marge haute ne peut pas être négative")
    @Max(value = 100, message = "La marge haute ne peut pas dépasser 100mm")
    private Integer margeHautMm;

    @Schema(description = "Marge basse en mm", example = "10")
    @Min(value = 0, message = "La marge basse ne peut pas être négative")
    @Max(value = 100, message = "La marge basse ne peut pas dépasser 100mm")
    private Integer margeBasMm;

    @Schema(description = "Couleur d'arrière-plan de la feuille (hex)", example = "#ffffff")
    private String couleurFond;

    // --- Header ---
    @Schema(description = "Activer l'en-tête répété sur chaque page", example = "true")
    private Boolean headerActif;

    @Schema(description = "Hauteur de l'en-tête en mm", example = "15")
    @Min(value = 5, message = "La hauteur du header doit être d'au moins 5mm")
    @Max(value = 100, message = "La hauteur du header ne peut pas dépasser 100mm")
    private Integer hauteurHeaderMm;

    @Schema(description = "Contenu de l'en-tête (texte ou HTML simple)", example = "RAPPORT D'ACTIVITÉ MENSUEL")
    private String headerContenu;

    @Schema(description = "Alignement du contenu de l'en-tête", example = "LEFT", allowableValues = {"LEFT", "CENTER", "RIGHT", "BETWEEN"})
    private String headerAlignement;

    @Schema(description = "Afficher l'en-tête sur la première page", example = "true")
    private Boolean headerAfficherSurPremierePage;

    @Schema(description = "Afficher une ligne de séparation sous l'en-tête", example = "true")
    private Boolean headerLigneSeparation;

    @Schema(description = "Couleur de la ligne de séparation de l'en-tête", example = "#d1d5db")
    private String headerCouleurLigne;

    // --- Footer ---
    @Schema(description = "Activer le pied de page répété sur chaque page", example = "true")
    private Boolean footerActif;

    @Schema(description = "Hauteur du pied de page en mm", example = "15")
    @Min(value = 5, message = "La hauteur du footer doit être d'au moins 5mm")
    @Max(value = 100, message = "La hauteur du footer ne peut pas dépasser 100mm")
    private Integer hauteurFooterMm;

    @Schema(description = "Contenu du pied de page (texte ou mentions légales)", example = "Confidentiel - Usage interne uniquement")
    private String footerContenu;

    @Schema(description = "Alignement du contenu du pied de page", example = "LEFT", allowableValues = {"LEFT", "CENTER", "RIGHT", "BETWEEN"})
    private String footerAlignement;

    @Schema(description = "Afficher le pied de page sur la première page", example = "true")
    private Boolean footerAfficherSurPremierePage;

    @Schema(description = "Afficher une ligne de séparation au-dessus du pied de page", example = "true")
    private Boolean footerLigneSeparation;

    @Schema(description = "Couleur de la ligne de séparation du pied de page", example = "#d1d5db")
    private String footerCouleurLigne;

    @Schema(description = "Activer la numérotation automatique des pages", example = "true")
    private Boolean numerotationPage;

    @Schema(description = "Format de la numérotation des pages", example = "PAGE_X_SUR_Y", allowableValues = {"PAGE_X_SUR_Y", "PAGE_X", "X_SUR_Y"})
    private String formatNumerotation;
}

