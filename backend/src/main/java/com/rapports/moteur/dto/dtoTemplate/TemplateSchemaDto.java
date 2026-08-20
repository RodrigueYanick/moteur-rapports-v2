package com.rapports.moteur.dto.dtoTemplate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;
import java.util.UUID;

import com.rapports.moteur.dto.dtoVariable.VariableResponse;

@Schema(description = "Dictionnaire de variables (schéma) d'un modèle publié")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class TemplateSchemaDto {

    @Schema(description = "Identifiant du modèle", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID templateId;

    @Schema(description = "Nom du modèle", example = "Facture Client")
    private String nom;

    @Schema(description = "Version du modèle", example = "1")
    private Integer version;

    @Schema(description = "Liste des variables attendues par le modèle")
    private List<VariableResponse> variables;
}