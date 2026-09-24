package com.rapports.moteur.dto.dtoBatch;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Demande de test d'une URL de webhook")
public class WebhookTestRequest {

    @NotBlank(message = "L'URL cible du webhook est obligatoire")
    @Schema(description = "URL HTTP/HTTPS à tester", example = "https://webhook.site/test-uuid")
    private String url;

    @Schema(description = "Secret HMAC-SHA256 optionnel pour tester la signature", example = "whsec_test123")
    private String secret;
}

