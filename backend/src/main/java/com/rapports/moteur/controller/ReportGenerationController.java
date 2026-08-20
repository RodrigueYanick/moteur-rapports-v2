package com.rapports.moteur.controller;

import com.rapports.moteur.dto.dtoGeneration.GenerationDto;
import com.rapports.moteur.service.ReportGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/generations")
@Tag(name = "Historique des générations", description = "Consultation de l'historique des documents générés par template")
public class ReportGenerationController {

    @Autowired
    private ReportGenerationService service;

    @Operation(
        summary = "Historique des générations d'un template",
        description = "Retourne la liste des générations effectuées pour un template donné, triées de la plus récente à la plus ancienne."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des générations",
                     content = @Content(schema = @Schema(implementation = GenerationDto.class))),
        @ApiResponse(responseCode = "400", description = "Paramètre templateId invalide",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Template introuvable",
                     content = @Content)
    })
    @GetMapping
    public List<GenerationDto> getGenerationsForTemplate(
            @Parameter(description = "Identifiant du template", required = true)
            @RequestParam @NonNull UUID templateId) {
        return service.getHistory(templateId);
    }

    // L’ancienne méthode getAllDocuments a été supprimée pour éviter le mapping dupliqué.
}