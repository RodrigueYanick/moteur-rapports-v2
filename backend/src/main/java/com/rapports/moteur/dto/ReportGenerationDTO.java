package com.rapports.moteur.dto;

import com.rapports.moteur.entity.GenerationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReportGenerationDTO(
        UUID id,
        UUID templateId,
        String title,
        String format,
        GenerationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
