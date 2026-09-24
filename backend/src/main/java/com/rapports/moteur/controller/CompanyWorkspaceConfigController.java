package com.rapports.moteur.controller;

import com.rapports.moteur.dto.ApiError;
import com.rapports.moteur.dto.dtoWorkspace.WorkspaceConfigRequest;
import com.rapports.moteur.dto.dtoWorkspace.WorkspaceConfigResponse;
import com.rapports.moteur.service.CompanyWorkspaceConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspace-config")
@RequiredArgsConstructor
@Tag(name = "Feuille de travail", description = "Personnalisation générale de la feuille de travail par entreprise : format, marges, header, footer, arrière-plan")
public class CompanyWorkspaceConfigController {

    private final CompanyWorkspaceConfigService workspaceConfigService;

    @Operation(
        summary = "Obtenir la configuration de la feuille de travail de l'entreprise",
        description = "Retourne la configuration par défaut de la feuille pour l'entreprise courante (déduite du header X-Entreprise-Code). Si aucune configuration n'est enregistrée, les valeurs par défaut standards sont renvoyées."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration récupérée avec succès",
                     content = @Content(schema = @Schema(implementation = WorkspaceConfigResponse.class)))
    })
    @GetMapping
    public ResponseEntity<WorkspaceConfigResponse> getConfig() {
        return ResponseEntity.ok(workspaceConfigService.getConfigForCurrentEntreprise());
    }

    @Operation(
        summary = "Enregistrer ou modifier la configuration de la feuille de travail",
        description = "Met à jour ou crée la configuration par défaut de la feuille pour l'entreprise courante. Tout nouveau template créé par cette entreprise héritera automatiquement de ces paramètres."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration enregistrée avec succès",
                     content = @Content(schema = @Schema(implementation = WorkspaceConfigResponse.class))),
        @ApiResponse(responseCode = "400", description = "Données de configuration invalides",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping
    public ResponseEntity<WorkspaceConfigResponse> updateConfig(@Valid @RequestBody WorkspaceConfigRequest request) {
        return ResponseEntity.ok(workspaceConfigService.saveOrUpdateConfig(request));
    }

    @Operation(
        summary = "Réinitialiser la configuration de la feuille aux paramètres d'usine",
        description = "Rétablit les paramètres standards par défaut (A4, marges de 10mm, sans en-tête ni pied de page) pour l'entreprise courante."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration réinitialisée",
                     content = @Content(schema = @Schema(implementation = WorkspaceConfigResponse.class)))
    })
    @PostMapping("/reset")
    public ResponseEntity<WorkspaceConfigResponse> resetConfig() {
        return ResponseEntity.ok(workspaceConfigService.resetToDefaults());
    }
}

