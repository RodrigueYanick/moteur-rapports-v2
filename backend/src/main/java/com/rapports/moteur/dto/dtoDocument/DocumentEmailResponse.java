package com.rapports.moteur.dto.dtoDocument;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Confirmation de l'envoi d'un document par email")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentEmailResponse {

    @Schema(description = "Indique si l'envoi a réussi", example = "true")
    private boolean succes;

    @Schema(description = "Message de statut ou de confirmation", example = "Document envoyé avec succès")
    private String message;

    @Schema(description = "Adresse email du destinataire", example = "client@entreprise.com")
    private String destinataire;

    @Schema(description = "Nom du document expédié", example = "Facture FAC-2026-001.pdf")
    private String nomDocument;

    @Schema(description = "Date et heure de l'envoi ISO", example = "2026-09-14T02:50:00")
    private String dateEnvoi;
}

