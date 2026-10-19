package com.rapports.moteur.dto.dtoAuth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnterpriseUpgradeRequest {

    @NotBlank(message = "Le nom de l'entreprise est obligatoire pour créer une organisation")
    @Size(min = 2, max = 150, message = "Le nom de l'entreprise doit contenir entre 2 et 150 caractères")
    private String nomEntreprise;

    @Size(max = 50, message = "Le code organisation ne peut pas dépasser 50 caractères")
    private String codeEntreprise;

    @Size(max = 30, message = "Le numéro SIRET/Identifiant ne peut pas dépasser 30 caractères")
    private String siret;

    @Size(max = 255, message = "L'adresse ne peut pas dépasser 255 caractères")
    private String adresse;

    @Size(max = 30, message = "Le numéro de téléphone ne peut pas dépasser 30 caractères")
    private String telephone;

    @Size(max = 150, message = "L'email de contact ne peut pas dépasser 150 caractères")
    private String emailContact;
}
