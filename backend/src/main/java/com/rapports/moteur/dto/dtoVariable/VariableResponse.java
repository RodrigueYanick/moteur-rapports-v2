package com.rapports.moteur.dto.dtoVariable;

import lombok.*;

import java.util.UUID;

import com.rapports.moteur.entity.VariableType;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class VariableResponse {
    private UUID id;
    private String nomVariable;
    private VariableType type;
    private Boolean obligatoire;
}