package com.rapports.moteur.dto.dtoCertificate;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateCreateRequest {

    @JsonAlias({"nom"})
    private String alias;

    private String nom;

    private String description;

    @JsonAlias({"fichierBase64", "file"})
    private String fichierCertificatBase64;

    private String fichierBase64;

    @NotBlank(message = "Le mot de passe du trousseau est obligatoire")
    @JsonAlias({"password"})
    private String motDePasse;

    public String getNom() {
        return (nom != null && !nom.isBlank()) ? nom : alias;
    }

    public String getAlias() {
        return (alias != null && !alias.isBlank()) ? alias : nom;
    }

    public String getFichierBase64() {
        return (fichierBase64 != null && !fichierBase64.isBlank()) ? fichierBase64 : fichierCertificatBase64;
    }

    public String getFichierCertificatBase64() {
        return (fichierCertificatBase64 != null && !fichierCertificatBase64.isBlank()) ? fichierCertificatBase64 : fichierBase64;
    }
}