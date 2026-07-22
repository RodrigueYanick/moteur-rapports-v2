package com.rapports.moteur.dto.dtoTemplate;

import lombok.*;

import java.util.List;
import java.util.UUID;

import com.rapports.moteur.dto.dtoVariable.VariableResponse;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class TemplateSchemaDto {
    private UUID templateId;
    private String nom;
    private Integer version;
    private List<VariableResponse> variables;
}