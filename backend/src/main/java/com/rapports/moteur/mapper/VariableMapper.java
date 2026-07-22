package com.rapports.moteur.mapper;

import org.springframework.stereotype.Component;

import com.rapports.moteur.dto.dtoVariable.VariableResponse;
import com.rapports.moteur.entity.ReportVariable;

@Component
public class VariableMapper {

    public VariableResponse toDto(ReportVariable variable) {
    return VariableResponse.builder()
            .id(variable.getId())
            .nomVariable(variable.getNomVariable())
            .type(variable.getType())
            .obligatoire(variable.getObligatoire())
            .build();
    }

}
