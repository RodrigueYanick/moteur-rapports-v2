package com.rapports.moteur.dto.dtoGeneration;

import com.rapports.moteur.entity.GenerationStatus;

import java.time.LocalDateTime;
// import java.util.UUID;

public record GenerationResponse(
        // UUID id,
        // UUID templateId,
        String title,
        String format,
        GenerationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
