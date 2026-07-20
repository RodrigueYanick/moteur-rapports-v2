package com.rapports.moteur.dto;

import com.rapports.moteur.entity.TemplateStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ReportTemplateDTO(
        UUID id,
        String name,
        String description,
        TemplateStatus status,
        List<ReportVariableDTO> variables,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
