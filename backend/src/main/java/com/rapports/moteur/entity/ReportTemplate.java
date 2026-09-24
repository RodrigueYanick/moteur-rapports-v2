package com.rapports.moteur.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.ColumnTransformer;
@Data
@Entity
@Table(name = "report_template")
@Getter 
@Setter
@NoArgsConstructor 
@AllArgsConstructor
@Builder
public class ReportTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nom;

    private String description;

    @Column(name = "contenu_design", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String contenuDesign;

    @Column(name = "schema", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String schema;

    @Column(name = "code_entreprise", length = 50)   // supprimer nullable = false
    private String codeEntreprise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateStatus statut;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @Enumerated(EnumType.STRING)
    @Column(name = "categorie", nullable = false, length = 50)
    @Builder.Default
    private Categorie categorie = Categorie.AUTRES;

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

    @Column(name = "marge_gauche_mm")
    @Builder.Default
    private Integer margeGaucheMm = 0;

    @Column(name = "marge_droite_mm")
    @Builder.Default
    private Integer margeDroiteMm = 0;

    @Column(name = "marge_haut_mm")
    @Builder.Default
    private Integer margeHautMm = 0;

    @Column(name = "marge_bas_mm")
    @Builder.Default
    private Integer margeBasMm = 0;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_template_id")
    private ReportTemplate parentTemplate;


    @PrePersist
    protected void onCreate() {
        dateCreation = LocalDateTime.now();
        dateModification = LocalDateTime.now();
        if (version == null) version = 1;
        if (statut == null) statut = TemplateStatus.BROUILLON;
    }

    @PreUpdate // Méthode appelée avant la mise à jour de l'entité
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }
}