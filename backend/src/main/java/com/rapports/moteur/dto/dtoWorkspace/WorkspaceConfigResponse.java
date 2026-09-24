package com.rapports.moteur.dto.dtoWorkspace;

import com.rapports.moteur.entity.PaginationMode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Réponse contenant la configuration de la feuille de travail pour une entreprise")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceConfigResponse {

    private UUID id;
    private String codeEntreprise;
    private String formatPapier;
    private Integer largeurMm;
    private Integer hauteurMm;
    private PaginationMode modePagination;
    private Integer margeGaucheMm;
    private Integer margeDroiteMm;
    private Integer margeHautMm;
    private Integer margeBasMm;
    private String couleurFond;

    // --- Header ---
    private Boolean headerActif;
    private Integer hauteurHeaderMm;
    private String headerContenu;
    private String headerAlignement;
    private Boolean headerAfficherSurPremierePage;
    private Boolean headerLigneSeparation;
    private String headerCouleurLigne;

    // --- Footer ---
    private Boolean footerActif;
    private Integer hauteurFooterMm;
    private String footerContenu;
    private String footerAlignement;
    private Boolean footerAfficherSurPremierePage;
    private Boolean footerLigneSeparation;
    private String footerCouleurLigne;
    private Boolean numerotationPage;
    private String formatNumerotation;

    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}

