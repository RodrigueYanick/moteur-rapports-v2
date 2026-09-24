package com.rapports.moteur.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "batch_generation_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchGenerationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private ReportBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id")
    private ReportGeneration generation;

    @Column(name = "custom_id", length = 100)
    private String customId;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private BatchItemStatus statut;

    @Column(name = "url_fichier", length = 500)
    private String urlFichier;

    @Column(name = "erreur", columnDefinition = "TEXT")
    private String erreur;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "donnees", columnDefinition = "jsonb")
    private String donnees;

    @Column(name = "date_traitement")
    private LocalDateTime dateTraitement;

    @PrePersist
    protected void onCreate() {
        if (statut == null) statut = BatchItemStatus.EN_ATTENTE;
    }
}

