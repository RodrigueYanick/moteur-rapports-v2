package com.rapports.moteur.dto.dtoBatch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Résultat du test de connectivité vers un webhook")
public class WebhookTestResponse {

    private boolean succes;
    private int statusCode;
    private String message;
    private long tempsReponseMs;
}

