package com.rapports.moteur.dto.dtoVariable;

import com.rapports.moteur.entity.VariableType;

// import java.util.UUID;

public record ReportVariableDTO(
        // UUID id,
        String name,
        VariableType type,
        String description,
        boolean required 
) {
}
