package com.rapports.moteur.dto.dtoVariable;

import com.rapports.moteur.entity.VariableType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "Requête de création ou de mise à jour d'une variable")
@Data
public class VariableRequest {

    @Schema(description = "Nom unique de la variable dans le modèle", example = "nom_client")
    @NotBlank
    private String nomVariable;

    @Schema(
        description = "Type de la variable",
        example = "STRING",
        allowableValues = {"STRING", "FLOAT", "DATE", "BOOLEAN", "ARRAY", "IMAGE"}
    )
    @NotNull
    private VariableType type;

    @Schema(description = "Indique si la variable est obligatoire", example = "true")
    private Boolean obligatoire = false;

    @Schema(description = "Description de la variable", example = "Nom complet du client")
    private String description;
}