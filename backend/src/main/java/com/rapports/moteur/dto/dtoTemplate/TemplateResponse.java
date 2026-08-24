package com.rapports.moteur.dto.dtoTemplate;

import com.rapports.moteur.entity.Categorie;
import com.rapports.moteur.entity.TemplateStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Représentation d'un modèle de rapport (template)")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class TemplateResponse {

    @Schema(description = "Identifiant unique du modèle", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Nom du modèle", example = "Facture Client")
    private String nom;

    @Schema(description = "Description du modèle", example = "Modèle de facture standard")
    private String description;

    @Schema(description = "Contenu du design au format JSON", example = "{\"pages\":[]}")
    private String contenuDesign;

    @Schema(description = "Catégorie du modèle", example = "VENTES", allowableValues = {
            "VENTES", "ACHATS", "FINANCE", "RH", "LOGISTIQUE", "STOCK", "PRODUCTION", "ADMINISTRATION", "AUTRES"})
    private Categorie categorie;

    @Schema(description = "Format papier du modèle", example = "A4", allowableValues = {"A4", "A5", "Letter", "Legal"})
    private String formatPapier;

    @Schema(description = "Largeur personnalisée en millimètres", example = "80")
    private Integer largeurMm;

    @Schema(description = "Hauteur personnalisée en millimètres", example = "200")
    private Integer hauteurMm;

    @Schema(description = "Statut actuel du modèle", example = "PUBLIE", allowableValues = {"BROUILLON", "PUBLIE", "ARCHIVE"})
    private TemplateStatus statut;

    @Schema(description = "Numéro de version du modèle", example = "1")
    private Integer version;

    @Schema(description = "Date de création", example = "2026-08-19T10:15:30")
    private LocalDateTime dateCreation;

    @Schema(description = "Date de dernière modification", example = "2026-08-19T14:22:10")
    private LocalDateTime dateModification;
}