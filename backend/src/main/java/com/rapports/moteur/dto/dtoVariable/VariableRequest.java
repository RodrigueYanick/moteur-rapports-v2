package com.rapports.moteur.dto.dtoVariable;

import com.rapports.moteur.entity.VariableType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VariableRequest {

    @NotBlank
    private String nomVariable;

    @NotNull
    private VariableType type;

    private Boolean obligatoire = false;

    private String description;
    
}