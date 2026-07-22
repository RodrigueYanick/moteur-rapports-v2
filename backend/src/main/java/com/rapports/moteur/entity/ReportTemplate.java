package com.rapports.moteur.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Data
@Entity
@Table(name = "report_templates")
public class ReportTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "contenu_design", nullable = false, columnDefinition = "TEXT")
    private String contenuDesign; // Le contenu du design du rapport

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateStatus status = TemplateStatus.DRAFT; // Statut du template (DRAFT, PUBLISHED, ARCHIVED)

    @Column(nullable = false)
    private Integer version = 1;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReportVariable> variables = new ArrayList<>();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generation_id")
    private ReportGeneration generation;


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist // Méthode appelée avant la persistance de l'entité
    protected void onCreate() { // Méthode appelée avant la persistance de l'entité
        createdAt = LocalDateTime.now(); // Initialisation de la date de création
        updatedAt = LocalDateTime.now(); // Initialisation de la date de mise à jour
    }

    @PreUpdate // Méthode appelée avant la mise à jour de l'entité
    protected void onUpdate() {
        updatedAt = LocalDateTime.now(); // Mise à jour de la date de mise à jour
    }

    // public UUID getId() { return id; }
    // public String getName() { return name; }
    // public void setName(String name) { this.name = name; }
    // public String getDescription() { return description; }
    // public void setDescription(String description) { this.description = description; }
    // public String getContenuDesign() { return contenuDesign; }
    // public void setContenuDesign(String contenuDesign) { this.contenuDesign = contenuDesign; }
    // public TemplateStatus getStatus() { return status; }
    // public void setStatus(TemplateStatus status) { this.status = status; }
    // public Integer getVersion() { return version; }
    // public void setVersion(Integer version) { this.version = version; }
    // public List<ReportVariable> getVariables() { return variables; }
    // public void setVariables(List<ReportVariable> variables) { this.variables = variables; }
    // public LocalDateTime getCreatedAt() { return createdAt; }
    // public LocalDateTime getUpdatedAt() { return updatedAt; }
}
