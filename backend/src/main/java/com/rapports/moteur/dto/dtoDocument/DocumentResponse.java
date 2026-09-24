package com.rapports.moteur.dto.dtoDocument;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Représentation d'un document généré")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentResponse {
    @Schema(description = "Identifiant unique du document", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Identifiant du template associé", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID templateId;

    @Schema(description = "Nom du document", example = "Facture Janvier 2026")
    private String nom;

    @Schema(description = "Nom du template source", example = "Facture Client")
    private String templateNom;

    @Schema(description = "Code de l'entreprise propriétaire du document", example = "ENT-001")
    private String codeEntreprise;

    @Schema(description = "Données du document (JSON)", type = "object", example = "{\"nom_client\":\"Entreprise ABC\"}")
    private Object donnees;

    @Schema(description = "Statut du document", example = "BROUILLON", allowableValues = {"BROUILLON", "FINALISE"})
    private String statut;

    @Schema(description = "Date de création", example = "2026-08-19T10:15:30")
    private LocalDateTime dateCreation;

    @Schema(description = "Date de dernière modification", example = "2026-08-19T14:22:10")
    private LocalDateTime dateModification;
}