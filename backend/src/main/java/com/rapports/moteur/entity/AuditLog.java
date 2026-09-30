package com.rapports.moteur.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "code_entreprise", length = 50)
    private String codeEntreprise;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "user_email", length = 100)
    private String userEmail;

    @Column(name = "user_role", length = 50)
    private String userRole;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "ressource_type", nullable = false, length = 50)
    private String ressourceType;

    @Column(name = "ressource_id", length = 100)
    private String ressourceId;

    @Column(name = "adresse_ip", length = 50)
    private String adresseIp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String statut = "SUCCES";
}