package com.rapports.moteur.controller;

import com.rapports.moteur.dto.dtoGeneration.GenerationDto;
import com.rapports.moteur.dto.dtoGeneration.GenerationResponse;
import com.rapports.moteur.dto.dtoTemplate.TemplateCreate;
import com.rapports.moteur.dto.dtoTemplate.TemplateResponse;
import com.rapports.moteur.dto.dtoTemplate.TemplateSchemaDto;
import com.rapports.moteur.dto.dtoVariable.VariableRequest;
import com.rapports.moteur.dto.dtoVariable.VariableResponse;
import com.rapports.moteur.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.lang.NonNull;

@RestController
@RequestMapping("/api/templates")
@Tag(name = "Modèles", description = "Gestion complète des modèles de rapports (création, publication, version, variables, génération)")
public class TemplateController {

    private final ReportTemplateService templateService;
    private final VariableService variableService;
    private final SchemaService schemaService;
    private final ReportGenerationService generationService;

    public TemplateController(ReportTemplateService templateService,
                              VariableService variableService,
                              SchemaService schemaService,
                              ReportGenerationService generationService) {
        this.templateService = templateService;
        this.variableService = variableService;
        this.schemaService = schemaService;
        this.generationService = generationService;
    }

    // ---------- CRUD template ----------


    @Operation(
        summary = "Créer un nouveau modèle",
        description = "Crée un template en mode brouillon avec les informations de base."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Modèle créé avec succès",
                     content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides",
                     content = @Content),
        @ApiResponse(responseCode = "401", description = "Non autorisé",
                     content = @Content)
    })
    @PostMapping
    public ResponseEntity<TemplateResponse> create(@Valid @RequestBody TemplateCreate request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.create(request));
    }

    @Operation(
        summary = "Lister tous les modèles",
        description = "Retourne tous les modèles accessibles pour l'entreprise associée au code entreprise fourni en en-tête."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des modèles",
                     content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
        @ApiResponse(responseCode = "400", description = "Code entreprise manquant ou invalide",
                     content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getAll() {
        return ResponseEntity.ok(templateService.findAll());
    }

    @Operation(
        summary = "Obtenir un modèle par ID",
        description = "Retourne les détails d'un template spécifique."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Modèle trouvé",
                     content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable",
                     content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> getById(
            @Parameter(description = "Identifiant du modèle", required = true)
            @PathVariable @NonNull UUID id) {
        return ResponseEntity.ok(templateService.findById(id));
    }

    @Operation(
        summary = "Mettre à jour un modèle",
        description = "Modifie les champs d'un template existant (uniquement s'il est en mode brouillon)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Modèle mis à jour",
                     content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides ou état incompatible",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable",
                     content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> update(
            @Parameter(description = "Identifiant du modèle", required = true)
            @PathVariable @NonNull UUID id,
            @Valid @RequestBody TemplateCreate request) {
        return ResponseEntity.ok(templateService.update(id, request));
    }

    @Operation(
        summary = "Supprimer un modèle",
        description = "Supprime définitivement un template, quel que soit son statut."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Modèle supprimé avec succès"),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable",
                     content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identifiant du modèle", required = true)
            @PathVariable @NonNull UUID id) {
        templateService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Publier un modèle",
        description = "Publie un template en mode brouillon, extrait automatiquement les variables du design et fige le schéma."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Modèle publié avec succès",
                     content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
        @ApiResponse(responseCode = "400", description = "Le modèle n'est pas en mode brouillon",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable",
                     content = @Content)
    })
    @PostMapping("/{id}/publish")
    public ResponseEntity<TemplateResponse> publish(
            @Parameter(description = "Identifiant du modèle", required = true)
            @PathVariable @NonNull UUID id) {
        return ResponseEntity.ok(templateService.publish(id));
    }

    // ---------- Variables ----------

    @Operation(
        summary = "Lister les variables d'un modèle",
        description = "Retourne la liste des variables définies pour un template donné."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des variables",
                     content = @Content(schema = @Schema(implementation = VariableResponse.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable",
                     content = @Content)
    })
    @GetMapping("/{templateId}/variables")
    public ResponseEntity<List<VariableResponse>> getVariables(
            @Parameter(description = "Identifiant du modèle", required = true)
            @PathVariable @NonNull UUID templateId) {
        return ResponseEntity.ok(variableService.getVariables(templateId));
    }

    @Operation(
        summary = "Ajouter une variable à un modèle",
        description = "Ajoute une variable (uniquement si le modèle est en mode brouillon)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Variable créée",
                     content = @Content(schema = @Schema(implementation = VariableResponse.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides ou doublon",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable",
                     content = @Content)
    })
    @PostMapping("/{templateId}/variables")
    public ResponseEntity<VariableResponse> addVariable(
            @Parameter(description = "Identifiant du modèle", required = true)
            @PathVariable @NonNull UUID templateId,
            @Valid @RequestBody VariableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(variableService.addVariable(templateId, request));
    }

    @Operation(
        summary = "Supprimer une variable",
        description = "Supprime une variable existante (uniquement si le modèle est en mode brouillon)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Variable supprimée avec succès"),
        @ApiResponse(responseCode = "400", description = "Variable non supprimable",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Variable ou modèle introuvable",
                     content = @Content)
    })
    @DeleteMapping("/{templateId}/variables/{variableId}")
    public ResponseEntity<Void> deleteVariable(
            @Parameter(description = "Identifiant du modèle", required = true)
            @PathVariable @NonNull UUID templateId,
            @Parameter(description = "Identifiant de la variable", required = true)
            @PathVariable @NonNull UUID variableId) {
        variableService.deleteVariable(templateId, variableId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Mettre à jour une variable",
        description = "Modifie les informations d'une variable existante (uniquement si le modèle est en mode brouillon)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Variable mise à jour",
                     content = @Content(schema = @Schema(implementation = VariableResponse.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides ou état incompatible",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Variable ou modèle introuvable",
                     content = @Content)
    })
    @PutMapping("/{templateId}/variables/{variableId}")
    public ResponseEntity<VariableResponse> updateVariable(
            @Parameter(description = "Identifiant du modèle", required = true)
            @PathVariable UUID templateId,
            @Parameter(description = "Identifiant de la variable", required = true)
            @PathVariable UUID variableId,
            @Valid @RequestBody VariableRequest request) {
        return ResponseEntity.ok(variableService.updateVariable(templateId, variableId, request));
    }

    // ---------- Schema ----------

    @Operation(
        summary = "Obtenir le dictionnaire des variables (schéma)",
        description = "Retourne le schéma JSON des variables attendues par un modèle publié, avec leur type et leur caractère obligatoire."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Schéma récupéré",
                     content = @Content(schema = @Schema(implementation = TemplateSchemaDto.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable",
                     content = @Content)
    })
    @GetMapping("/{id}/schema")
    public ResponseEntity<TemplateSchemaDto> schema(
            @Parameter(description = "Identifiant du modèle", required = true)
            @PathVariable @NonNull UUID id) {
        return ResponseEntity.ok(schemaService.getSchema(id));
    }

    // ---------- Génération synchrone ----------

    @Operation(
        summary = "Générer un document PDF",
        description = "Reçoit les données réelles, valide qu'elles correspondent au schéma du modèle, puis génère le PDF final."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "PDF généré avec succès",
                     content = @Content(mediaType = "application/pdf")),
        @ApiResponse(responseCode = "400", description = "Données invalides ou champ manquant",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable ou non publié",
                     content = @Content)
    })
    @PostMapping("/{id}/generate")
    public ResponseEntity<byte[]> generate(
            @Parameter(description = "Identifiant du modèle publié", required = true)
            @PathVariable @NonNull UUID id,
            @RequestBody Map<String, Object> data) {
        byte[] pdf = generationService.generateSync(id, data);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rapport.pdf\"")
            .contentType(java.util.Objects.requireNonNull(MediaType.APPLICATION_PDF))
            .body(pdf);
    }

    @Operation(
        summary = "Aperçu HTML d'un document",
        description = "Retourne le HTML généré à partir des données et du design, sans conversion PDF. Utile pour le débogage ou l'aperçu."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "HTML généré",
                     content = @Content(mediaType = "text/html")),
        @ApiResponse(responseCode = "400", description = "Données invalides",
                     content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable",
                     content = @Content)
    })
    @PostMapping("/{id}/preview-html")
    public ResponseEntity<String> previewHtml(
            @Parameter(description = "Identifiant du modèle", required = true)
            @PathVariable @NonNull UUID id,
            @RequestBody Map<String, Object> data) {
        String html = generationService.generateHtml(id, data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                .body(html);
    }

    // ---------- Génération asynchrone ----------

    @Operation(
        summary = "Générer un document PDF (asynchrone)",
        description = "Lance la génération en arrière-plan et retourne immédiatement un identifiant de tâche."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Tâche de génération acceptée",
                     content = @Content(schema = @Schema(implementation = GenerationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable",
                     content = @Content)
    })
    @PostMapping("/{id}/generate-async")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GenerationResponse generateAsync(
            @Parameter(description = "Identifiant du modèle publié", required = true)
            @PathVariable @NonNull UUID id,
            @RequestBody Map<String, Object> data) {
        return generationService.generateAsync(id, data);
    }

    // ---------- Historique ----------

    @Operation(
        summary = "Historique des générations d'un modèle",
        description = "Retourne la liste des générations effectuées pour un template donné."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des générations",
                     content = @Content(schema = @Schema(implementation = GenerationDto.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable",
                     content = @Content)
    })
    @GetMapping("/{id}/generations")
    public ResponseEntity<List<GenerationDto>> generations(
            @Parameter(description = "Identifiant du modèle", required = true)
            @PathVariable @NonNull UUID id) {
        return ResponseEntity.ok(generationService.getHistory(id));
    }

    @Operation(
        summary = "Dupliquer un modèle",
        description = "Crée une copie indépendante du modèle (statut brouillon, version 1)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Copie créée",
                     content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
        @ApiResponse(responseCode = "404", description = "Modèle original introuvable",
                     content = @Content)
    })
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<TemplateResponse> duplicate(
            @Parameter(description = "Identifiant du modèle à dupliquer", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.duplicate(id));
    }

    @Operation(
        summary = "Archiver un modèle",
        description = "Passe un modèle en statut archivé. Il n'est plus modifiable, mais reste consultable."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Modèle archivé",
                     content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
        @ApiResponse(responseCode = "400", description = "Le modèle est déjà archivé ou état incompatible",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable",
                     content = @Content)
    })
    @PostMapping("/{id}/archive")
    public ResponseEntity<TemplateResponse> archive(
            @Parameter(description = "Identifiant du modèle", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(templateService.archive(id));
    }

    @Operation(
        summary = "Créer une nouvelle version",
        description = "Archive le modèle actuel et crée un nouveau brouillon avec une version incrémentée."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Nouvelle version créée",
                     content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable",
                     content = @Content)
    })
    @PostMapping("/{id}/new-version")
    public ResponseEntity<TemplateResponse> newVersion(
            @Parameter(description = "Identifiant du modèle publié ou archivé", required = true)
            @PathVariable UUID id) {
        TemplateResponse newVersion = templateService.newVersion(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(newVersion);
    }

    @Operation(
        summary = "Restaurer un modèle archivé",
        description = "Repasse un modèle archivé en mode brouillon pour le modifier à nouveau."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Modèle restauré",
                     content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
        @ApiResponse(responseCode = "400", description = "Le modèle n'est pas archivé",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable",
                     content = @Content)
    })
    @PostMapping("/{id}/restore")
    public ResponseEntity<TemplateResponse> restore(
            @Parameter(description = "Identifiant du modèle archivé", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(templateService.restore(id));
    }
}