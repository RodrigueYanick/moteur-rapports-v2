package com.rapports.moteur.dto.dtoVariable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.UUID;
import com.rapports.moteur.entity.VariableType;

@Schema(description = "Représentation d'une variable définie dans un modèle")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class VariableResponse {

    @Schema(description = "Identifiant unique de la variable", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Nom de la variable", example = "nom_client")
    private String nomVariable;

    @Schema(
        description = "Type de la variable",
        example = "STRING",
        allowableValues = {"STRING", "FLOAT", "DATE", "BOOLEAN", "ARRAY", "IMAGE"}
    )
    private VariableType type;

    @Schema(description = "Indique si la variable est obligatoire", example = "true")
    private Boolean obligatoire;

    @Schema(description = "Description de la variable", example = "Nom complet du client")
    private String description;
}