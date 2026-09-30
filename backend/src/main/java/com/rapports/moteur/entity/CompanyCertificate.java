package com.rapports.moteur.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_certificate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyCertificate {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code_entreprise", nullable = false, length = 50)
    private String codeEntreprise;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String type = "PKCS12";

    @Column(name = "fichier_certificat_base64", nullable = false, columnDefinition = "TEXT")
    private String fichierCertificatBase64;

    @Column(name = "mot_de_passe_chiffre", nullable = false, length = 255)
    private String motDePasseChiffre;

    @Column(name = "alias_certificat", length = 100)
    private String aliasCertificat;

    @Column(length = 255)
    private String emetteur;

    @Column(length = 255)
    private String sujet;

    @Column(name = "date_expiration")
    private LocalDateTime dateExpiration;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @CreationTimestamp
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification")
    private LocalDateTime dateModification;
}