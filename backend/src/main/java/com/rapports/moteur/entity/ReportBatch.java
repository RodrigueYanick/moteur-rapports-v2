package com.rapports.moteur.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "report_batch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ReportTemplate template;

    @Column(name = "code_entreprise", length = 50)
    private String codeEntreprise;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private BatchStatus statut;

    @Column(name = "total_items", nullable = false)
    @Builder.Default
    private int totalItems = 0;

    @Column(name = "processed_items", nullable = false)
    @Builder.Default
    private int processedItems = 0;

    @Column(name = "success_count", nullable = false)
    @Builder.Default
    private int successCount = 0;

    @Column(name = "failure_count", nullable = false)
    @Builder.Default
    private int failureCount = 0;

    @Column(name = "webhook_url", length = 1000)
    private String webhookUrl;

    @Column(name = "webhook_secret", length = 255)
    private String webhookSecret;

    @Enumerated(EnumType.STRING)
    @Column(name = "webhook_statut", nullable = false, length = 30)
    @Builder.Default
    private WebhookStatus webhookStatut = WebhookStatus.NON_CONFIGURE;

    @Column(name = "webhook_tentatives", nullable = false)
    @Builder.Default
    private int webhookTentatives = 0;

    @Column(name = "erreur", columnDefinition = "TEXT")
    private String erreur;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BatchGenerationItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) dateCreation = LocalDateTime.now();
        if (statut == null) statut = BatchStatus.EN_ATTENTE;
        if (webhookStatut == null) {
            webhookStatut = (webhookUrl != null && !webhookUrl.isBlank()) 
                    ? WebhookStatus.EN_ATTENTE 
                    : WebhookStatus.NON_CONFIGURE;
        }
    }
}

