package com.rapports.moteur.dto.dtoVariable;

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

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractedVariable {
    private String nom;
    private String type;
    private Boolean obligatoire;
}
