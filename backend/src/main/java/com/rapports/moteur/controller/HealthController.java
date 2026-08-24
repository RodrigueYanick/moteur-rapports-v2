package com.rapports.moteur.controller;

import com.rapports.moteur.dto.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Santé", description = "Vérification de l'état de fonctionnement de l'API")
public class HealthController {

    @Operation(
        summary = "Vérifier la santé de l'API",
        description = """
            Retourne un indicateur simple de l'état du service.
            Utile pour les tests de disponibilité et la supervision.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Le service est opérationnel",
            content = @Content(
                schema = @Schema(
                    type = "object",
                    example = "{\"status\":\"UP\"}"
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Erreur interne inattendue",
            content = @Content(schema = @Schema(implementation = ApiError.class))
        )
    })
    @GetMapping
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}