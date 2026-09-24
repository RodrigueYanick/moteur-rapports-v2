package com.rapports.moteur.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_workspace_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyWorkspaceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code_entreprise", length = 50, nullable = false, unique = true)
    private String codeEntreprise;

    @Column(name = "format_papier", nullable = false, length = 10)
    @Builder.Default
    private String formatPapier = "A4";

    @Column(name = "largeur_mm")
    private Integer largeurMm;

    @Column(name = "hauteur_mm")
    private Integer hauteurMm;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_pagination", nullable = false, length = 20)
    @Builder.Default
    private PaginationMode modePagination = PaginationMode.FIXED;

    @Column(name = "marge_gauche_mm", nullable = false)
    @Builder.Default
    private Integer margeGaucheMm = 10;

    @Column(name = "marge_droite_mm", nullable = false)
    @Builder.Default
    private Integer margeDroiteMm = 10;

    @Column(name = "marge_haut_mm", nullable = false)
    @Builder.Default
    private Integer margeHautMm = 10;

    @Column(name = "marge_bas_mm", nullable = false)
    @Builder.Default
    private Integer margeBasMm = 10;

    @Column(name = "couleur_fond", length = 30)
    @Builder.Default
    private String couleurFond = "#ffffff";

    // --- Header ---
    @Column(name = "header_actif", nullable = false)
    @Builder.Default
    private Boolean headerActif = false;

    @Column(name = "hauteur_header_mm")
    @Builder.Default
    private Integer hauteurHeaderMm = 15;

    @Column(name = "header_contenu", columnDefinition = "text")
    private String headerContenu;

    @Column(name = "header_alignement", length = 20)
    @Builder.Default
    private String headerAlignement = "LEFT";

    @Column(name = "header_afficher_sur_premiere_page", nullable = false)
    @Builder.Default
    private Boolean headerAfficherSurPremierePage = true;

    @Column(name = "header_ligne_separation", nullable = false)
    @Builder.Default
    private Boolean headerLigneSeparation = false;

    @Column(name = "header_couleur_ligne", length = 30)
    @Builder.Default
    private String headerCouleurLigne = "#d1d5db";

    // --- Footer ---
    @Column(name = "footer_actif", nullable = false)
    @Builder.Default
    private Boolean footerActif = false;

    @Column(name = "hauteur_footer_mm")
    @Builder.Default
    private Integer hauteurFooterMm = 15;

    @Column(name = "footer_contenu", columnDefinition = "text")
    private String footerContenu;

    @Column(name = "footer_alignement", length = 20)
    @Builder.Default
    private String footerAlignement = "LEFT";

    @Column(name = "footer_afficher_sur_premiere_page", nullable = false)
    @Builder.Default
    private Boolean footerAfficherSurPremierePage = true;

    @Column(name = "footer_ligne_separation", nullable = false)
    @Builder.Default
    private Boolean footerLigneSeparation = false;

    @Column(name = "footer_couleur_ligne", length = 30)
    @Builder.Default
    private String footerCouleurLigne = "#d1d5db";

    @Column(name = "numerotation_page", nullable = false)
    @Builder.Default
    private Boolean numerotationPage = true;

    @Column(name = "format_numerotation", length = 50)
    @Builder.Default
    private String formatNumerotation = "PAGE_X_SUR_Y";

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        this.dateModification = LocalDateTime.now();
        if (this.formatPapier == null) this.formatPapier = "A4";
        if (this.modePagination == null) this.modePagination = PaginationMode.FIXED;
        if (this.margeGaucheMm == null) this.margeGaucheMm = 10;
        if (this.margeDroiteMm == null) this.margeDroiteMm = 10;
        if (this.margeHautMm == null) this.margeHautMm = 10;
        if (this.margeBasMm == null) this.margeBasMm = 10;
        if (this.couleurFond == null) this.couleurFond = "#ffffff";
        if (this.headerActif == null) this.headerActif = false;
        if (this.hauteurHeaderMm == null) this.hauteurHeaderMm = 15;
        if (this.headerAlignement == null) this.headerAlignement = "LEFT";
        if (this.headerAfficherSurPremierePage == null) this.headerAfficherSurPremierePage = true;
        if (this.headerLigneSeparation == null) this.headerLigneSeparation = false;
        if (this.headerCouleurLigne == null) this.headerCouleurLigne = "#d1d5db";
        if (this.footerActif == null) this.footerActif = false;
        if (this.hauteurFooterMm == null) this.hauteurFooterMm = 15;
        if (this.footerAlignement == null) this.footerAlignement = "LEFT";
        if (this.footerAfficherSurPremierePage == null) this.footerAfficherSurPremierePage = true;
        if (this.footerLigneSeparation == null) this.footerLigneSeparation = false;
        if (this.footerCouleurLigne == null) this.footerCouleurLigne = "#d1d5db";
        if (this.numerotationPage == null) this.numerotationPage = true;
        if (this.formatNumerotation == null) this.formatNumerotation = "PAGE_X_SUR_Y";
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = LocalDateTime.now();
    }
}

