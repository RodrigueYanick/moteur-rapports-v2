package com.rapports.moteur.dto.dtoCertificate;

import com.rapports.moteur.entity.CompanyCertificate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateResponse {
    private UUID id;
    private String codeEntreprise;
    private String nom;
    private String description;
    private String type;
    private String aliasCertificat;
    private String emetteur;
    private String sujet;
    private LocalDateTime dateExpiration;
    private boolean actif;
    private boolean expire;
    private LocalDateTime dateCreation;

    public static CertificateResponse fromEntity(CompanyCertificate entity) {
        boolean isExpired = entity.getDateExpiration() != null && entity.getDateExpiration().isBefore(LocalDateTime.now());
        return CertificateResponse.builder()
                .id(entity.getId())
                .codeEntreprise(entity.getCodeEntreprise())
                .nom(entity.getNom())
                .description(entity.getDescription())
                .type(entity.getType())
                .aliasCertificat(entity.getAliasCertificat())
                .emetteur(entity.getEmetteur())
                .sujet(entity.getSujet())
                .dateExpiration(entity.getDateExpiration())
                .actif(entity.isActif())
                .expire(isExpired)
                .dateCreation(entity.getDateCreation())
                .build();
    }

    public String getAlias() {
        return (aliasCertificat != null && !aliasCertificat.isBlank()) ? aliasCertificat : nom;
    }

    public boolean isActive() {
        return actif;
    }

    public boolean isExpired() {
        return expire;
    }

    public LocalDateTime getValidTo() {
        return dateExpiration;
    }

    public String getFilename() {
        return (nom != null && !nom.isBlank()) ? nom : "certificat.p12";
    }
}