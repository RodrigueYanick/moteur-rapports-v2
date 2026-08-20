package com.rapports.moteur.dto.dtoVariable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Variable détectée automatiquement dans le design d'un template (contenuDesign)
 * lors de sa publication.
 *
 * Contrairement à ReportVariable, elle n'est pas persistée individuellement en base :
 * la liste complète est sérialisée en JSON dans le champ ReportTemplate.schema.
 */
@Schema(description = "Variable extraite automatiquement du design d'un modèle")
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractedVariable {

    @Schema(description = "Nom de la variable extraite", example = "nom_client")
    private String nom;

    @Schema(description = "Type de la variable extraite", example = "STRING",
            allowableValues = {"STRING", "FLOAT", "DATE", "BOOLEAN", "ARRAY", "IMAGE"})
    private String type;

    @Schema(description = "Indique si la variable est obligatoire", example = "true")
    private Boolean obligatoire;
}