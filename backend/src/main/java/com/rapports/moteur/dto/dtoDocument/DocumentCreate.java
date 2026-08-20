package com.rapports.moteur.dto.dtoDocument;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "Requête de création ou de mise à jour d'un document")
@Data
public class DocumentCreate {

    @Schema(description = "Nom du document", example = "Facture Janvier 2026")
    @NotBlank
    private String nom;

    @Schema(
        description = "Données du document au format JSON. Les clés correspondent aux variables du template.",
        example = """
            {
              "nom_client": "Entreprise ABC",
              "date_emission": "2026-08-19",
              "lignes_commande": [
                {
                  "designation": "Produit A",
                  "qte": 3,
                  "pu": 12000,
                  "total": 36000
                }
              ]
            }
            """
    )
    @NotNull
    private Object donnees;
}