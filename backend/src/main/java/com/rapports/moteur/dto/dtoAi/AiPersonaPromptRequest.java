package com.rapports.moteur.dto.dtoAi;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiPersonaPromptRequest {

    @NotBlank(message = "Le type de persona est requis (CV_CANADIAN, CV_ATS, STAGE_REPORT, FREELANCE_INVOICE, MARKETING_FLYER)")
    private String personaType;

    @NotBlank(message = "Le prompt utilisateur ne peut pas être vide")
    @Size(max = 4000, message = "Le prompt ne doit pas dépasser 4000 caractères")
    private String userPrompt;

    @Builder.Default
    private String targetLanguage = "FR";
}

