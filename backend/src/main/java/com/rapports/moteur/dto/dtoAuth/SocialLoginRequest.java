package com.rapports.moteur.dto.dtoAuth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialLoginRequest {

    @NotBlank(message = "Le fournisseur d'identité (provider) est obligatoire (ex: GOOGLE, GITHUB)")
    private String provider;

    @NotBlank(message = "L'adresse email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    private String socialId;

    private String nom;

    private String prenom;

    private String avatarUrl;

    private String idToken;
}
