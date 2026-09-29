package com.rapports.moteur.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "scheduled_report_job")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledReportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ReportTemplate template;

    @Column(name = "code_entreprise", nullable = false, length = 50)
    private String codeEntreprise;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "cron_expression", nullable = false, length = 100)
    private String cronExpression;

    @Column(name = "fuseau_horaire", nullable = false, length = 50)
    @Builder.Default
    private String fuseauHoraire = "UTC";

    @Enumerated(EnumType.STRING)
    @Column(name = "format_export", nullable = false, length = 20)
    @Builder.Default
    private ReportExportFormat formatExport = ReportExportFormat.PDF;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parametres", columnDefinition = "jsonb")
    private String parametres;

    @Column(name = "destinataires_emails", columnDefinition = "text")
    private String destinatairesEmails;

    @Column(name = "webhook_url", length = 1000)
    private String webhookUrl;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @Column(name = "prochaine_execution")
    private LocalDateTime prochaineExecution;

    @Column(name = "derniere_execution")
    private LocalDateTime derniereExecution;

    @Column(name = "dernier_statut", length = 30)
    private String dernierStatut;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        this.dateModification = LocalDateTime.now();
        if (this.actif == null) {
            this.actif = true;
        }
        if (this.fuseauHoraire == null || this.fuseauHoraire.isBlank()) {
            this.fuseauHoraire = "UTC";
        }
        if (this.formatExport == null) {
            this.formatExport = ReportExportFormat.PDF;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = LocalDateTime.now();
    }
}
