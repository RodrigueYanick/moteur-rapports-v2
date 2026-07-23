package com.rapports.moteur.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Data
@Entity
@Table(name = "report_generation")
@Getter
@Setter
@NoArgsConstructor 
@AllArgsConstructor
@Builder
public class ReportGeneration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ReportTemplate template;

    @Column(name = "date_generation")
    private LocalDateTime dateGeneration;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "donnees_recues", columnDefinition = "jsonb")
    private String donneesRecues;   // JSON reçu de l'ERP, stocké en texte

    @Enumerated(EnumType.STRING)
    @Column(name = "statut")
    private GenerationStatus statut;

    @Column(name = "url_fichier_genere")
    private String urlFichierGenere;

    @PrePersist
    protected void onCreate() {
        dateGeneration = LocalDateTime.now();
        if (statut == null) statut = GenerationStatus.EN_COURS;
    }
}