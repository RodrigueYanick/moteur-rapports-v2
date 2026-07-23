package com.rapports.moteur.mapper;

import org.springframework.stereotype.Component;

import com.rapports.moteur.dto.dtoVariable.VariableRequest;
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

    public ReportVariable toEntity(VariableRequest create) {
        ReportVariable variable = new ReportVariable();
        variable.setNomVariable(create.getNomVariable());
        variable.setType(create.getType());
        variable.setObligatoire(create.getObligatoire() != null ? create.getObligatoire() : false);
        return variable;
    }

}
