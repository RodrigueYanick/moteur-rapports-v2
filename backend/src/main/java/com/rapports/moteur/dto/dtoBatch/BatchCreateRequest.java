package com.rapports.moteur.dto.dtoBatch;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Demande de lancement d'une génération par lot")
public class BatchCreateRequest {

    @Schema(description = "Identifiant du modèle de rapport à utiliser (optionnel si spécifié dans le chemin URL)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID templateId;

    @NotEmpty(message = "La liste des éléments du lot ne peut pas être vide")
    @Valid
    @Schema(description = "Liste ordonnée des jeux de données à générer")
    private List<BatchItemRequest> items;

    @Schema(description = "URL du webhook de rappel appelée à la fin du traitement (HTTP POST)", example = "https://erp.monentreprise.com/api/webhooks/rapports")
    private String webhookUrl;

    @Schema(description = "Clé secrète partagée pour la signature cryptographique HMAC-SHA256", example = "whsec_abcd1234efgh5678")
    private String webhookSecret;
}

