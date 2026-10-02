package com.rapports.moteur.dto.dtoAi;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTemplateGenerationResponse {
    private UUID templateId;
    private String nom;
    private String description;
    private String categorie;
    private String formatPapier;
    private String contenuDesign;
    private List<String> extractedVariables;
    private String promptUsed;
    private boolean fromMock;
    private String provider;
}
