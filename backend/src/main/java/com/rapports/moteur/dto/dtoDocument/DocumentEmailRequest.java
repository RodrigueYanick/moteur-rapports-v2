package com.rapports.moteur.dto.dtoDocument;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Requête d'envoi d'un document par email avec pièce jointe PDF")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEmailRequest {

    @Schema(description = "Adresse email du destinataire", example = "client@entreprise.com")
    @NotBlank(message = "L'adresse email du destinataire est requise")
    @Email(message = "Format d'adresse email invalide")
    private String destinataire;

    @Schema(description = "Objet de l'email", example = "Votre facture n° FAC-2026-001")
    @NotBlank(message = "L'objet de l'email est requis")
    private String objet;

    @Schema(description = "Corps du message de l'email", example = "Veuillez trouver ci-joint votre document.")
    private String message;
}

