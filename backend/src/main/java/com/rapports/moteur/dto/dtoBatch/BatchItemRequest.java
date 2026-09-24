package com.rapports.moteur.dto.dtoBatch;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Données d'un élément individuel dans une demande de génération par lot")
public class BatchItemRequest {

    @Schema(description = "Identifiant personnalisé ou référence externe (ex: FACT-2026-001, CLI-42)", example = "FACT-2026-001")
    private String customId;

    @NotNull(message = "Les données de l'élément sont obligatoires")
    @Schema(description = "Variables et données spécifiques à cet élément du rapport")
    private Map<String, Object> data;
}

