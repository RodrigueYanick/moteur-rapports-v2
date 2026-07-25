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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateStatus statut;

    @Column(nullable = false)
    private Integer version;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

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