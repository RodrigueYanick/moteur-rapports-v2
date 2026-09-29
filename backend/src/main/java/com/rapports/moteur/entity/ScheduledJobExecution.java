package com.rapports.moteur.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "scheduled_job_execution")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledJobExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private ScheduledReportJob job;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private JobExecutionStatus statut;

    @Column(name = "destinataires_notifies")
    @Builder.Default
    private Integer destinatairesNotifies = 0;

    @Column(name = "duree_ms")
    private Long dureeMs;

    @Column(name = "message_erreur", columnDefinition = "text")
    private String messageErreur;
}
