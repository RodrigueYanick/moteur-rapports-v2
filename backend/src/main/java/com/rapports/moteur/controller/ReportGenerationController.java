package com.rapports.moteur.controller;

import com.rapports.moteur.dto.ApiError;
import com.rapports.moteur.dto.dtoGeneration.GenerationDto;
import com.rapports.moteur.service.ReportGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/generations")
@Tag(name = "Historique des générations", description = "Consultation de l'historique des générations de documents pour un modèle donné")
public class ReportGenerationController {

    private final ReportGenerationService service;

    public ReportGenerationController(ReportGenerationService service) {
        this.service = service;
    }

    @Operation(
        summary = "Historique des générations d'un modèle",
        description = """
            Retourne la liste des générations effectuées pour un modèle donné,
            triées de la plus récente à la plus ancienne.
            
            Ce endpoint est utile pour :
            - consulter les documents déjà générés,
            - récupérer le statut d'une génération asynchrone,
            - obtenir les identifiants pour télécharger les PDF.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des générations",
                     content = @Content(schema = @Schema(implementation = GenerationDto.class))),
        @ApiResponse(responseCode = "400", description = "Paramètre templateId invalide (UUID mal formé)",
                     content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable ou inaccessible",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public List<GenerationDto> getGenerationsForTemplate(
            @Parameter(
                description = "Identifiant UUID du modèle dont on veut l'historique",
                required = true,
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
            )
            @RequestParam @NonNull UUID templateId) {
        return service.getHistory(templateId);
    }
}