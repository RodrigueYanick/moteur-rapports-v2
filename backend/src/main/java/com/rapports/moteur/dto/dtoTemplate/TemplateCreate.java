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

    @Schema(description = "Couleur d'arrière-plan de la feuille", example = "#ffffff")
    private String couleurFond;

    // --- Header ---
    @Schema(description = "Activer l'en-tête répété", example = "true")
    private Boolean headerActif;

    @Schema(description = "Hauteur de l'en-tête en mm", example = "15")
    private Integer hauteurHeaderMm;

    @Schema(description = "Contenu de l'en-tête (texte ou HTML)")
    private String headerContenu;

    @Schema(description = "Alignement de l'en-tête", example = "LEFT")
    private String headerAlignement;

    @Schema(description = "Afficher l'en-tête sur la première page", example = "true")
    private Boolean headerAfficherSurPremierePage;

    @Schema(description = "Ligne de séparation sous l'en-tête", example = "false")
    private Boolean headerLigneSeparation;

    @Schema(description = "Couleur de la ligne sous l'en-tête", example = "#d1d5db")
    private String headerCouleurLigne;

    // --- Footer ---
    @Schema(description = "Activer le pied de page répété", example = "true")
    private Boolean footerActif;

    @Schema(description = "Hauteur du pied de page en mm", example = "15")
    private Integer hauteurFooterMm;

    @Schema(description = "Contenu du pied de page (texte ou mentions légales)")
    private String footerContenu;

    @Schema(description = "Alignement du pied de page", example = "LEFT")
    private String footerAlignement;

    @Schema(description = "Afficher le pied de page sur la première page", example = "true")
    private Boolean footerAfficherSurPremierePage;

    @Schema(description = "Ligne de séparation au-dessus du pied de page", example = "false")
    private Boolean footerLigneSeparation;

    @Schema(description = "Couleur de la ligne au-dessus du pied de page", example = "#d1d5db")
    private String footerCouleurLigne;

    @Schema(description = "Activer la numérotation automatique des pages", example = "true")
    private Boolean numerotationPage;

    @Schema(description = "Format de la numérotation des pages", example = "PAGE_X_SUR_Y")
    private String formatNumerotation;
}