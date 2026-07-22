package com.rapports.moteur.dto.dtoGeneration;

import lombok.*;

import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GenerationResponse {
    private UUID generationId;
    private String status;   // "EN_COURS"
}