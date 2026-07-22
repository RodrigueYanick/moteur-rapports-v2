package com.rapports.moteur.dto.dtoTemplate;

import com.rapports.moteur.dto.dtoVariable.ReportVariableDTO;
import com.rapports.moteur.entity.TemplateStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TemplateResponse(
        UUID id,
        String name,
        String description,
        String contenuDesign,
        TemplateStatus status,
        Integer version,
        List<ReportVariableDTO> variables,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
