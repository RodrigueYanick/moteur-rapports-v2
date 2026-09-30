package com.rapports.moteur.dto.dtoCertificate;

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
    @NotBlank(message = "Le nom du certificat est obligatoire")
    private String nom;
    private String description;
    @NotBlank(message = "Le fichier certificat PKCS#12 encodÃ© en base64 est obligatoire")
    private String fichierBase64;
    @NotBlank(message = "Le mot de passe du trousseau est obligatoire")
    private String motDePasse;
}