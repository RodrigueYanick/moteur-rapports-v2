package com.rapports.moteur.dto.dtoAi;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPersonaGenerationResponse {
    private String personaType;
    private String nom;
    private String description;
    private String categorie;
    private String formatPapier;
    private String contenuDesign;
    private String schema;
    private Map<String, Object> sampleData;
    private String promptUsed;
    private boolean fromMock;
    private String provider;
}

