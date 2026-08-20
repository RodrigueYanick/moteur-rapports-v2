package com.rapports.moteur.dto.dtoGeneration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import com.rapports.moteur.entity.GenerationStatus;

@Schema(description = "Représentation d'une génération de document (historique)")
@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class GenerationDto {

    @Schema(description = "Identifiant unique de la génération", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Identifiant du template associé", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID templateId;

    @Schema(description = "Date et heure de la génération", example = "2026-08-19T14:15:48")
    private LocalDateTime dateGeneration;

    @Schema(description = "Données reçues pour la génération (JSON brut)", example = "{\"nom_client\":\"Entreprise ABC\"}")
    private String donneesRecues;

    @Schema(description = "Statut de la génération", example = "SUCCES")
    private GenerationStatus statut;

    @Schema(description = "Chemin ou URL du fichier PDF généré", example = "./data/reports/3fa85f64-5717-4562-b3fc-2c963f66afa6.pdf")
    private String urlFichierGenere;
}