package com.rapports.moteur.dto;

import com.rapports.moteur.entity.VariableType;

import java.util.UUID;

public record ReportVariableDTO(
        UUID id,
        String name,
        VariableType type,
        String defaultValue,
        boolean required
) {
}
