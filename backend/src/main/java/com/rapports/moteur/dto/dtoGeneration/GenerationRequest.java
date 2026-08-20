package com.rapports.moteur.dto.dtoGeneration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Requête de génération d'un document (données libres fournies par l'ERP)")
@Data
public class GenerationRequest {

    @Schema(
        description = "Données réelles à injecter dans le template. Les clés doivent correspondre aux variables définies.",
        example = """
            {
              "nom_client": "Entreprise ABC",
              "date_emission": "2026-08-19",
              "lignes_commande": [
                { "designation": "Produit A", "qte": 3, "pu": 12000, "total": 36000 }
              ]
            }
            """
    )
    private Object data;
}