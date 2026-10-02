package com.rapports.moteur.dto.dtoAi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTemplatePromptRequest {
    @NotBlank(message = "Le prompt de génération ne peut pas être vide")
    @Size(max = 2000, message = "Le prompt ne doit pas dépasser 2000 caractères")
    private String prompt;

    private String nom;
    private String categorie;
    private String formatPapier;
}
