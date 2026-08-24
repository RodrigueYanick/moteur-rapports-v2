package com.rapports.moteur.controller;

import com.rapports.moteur.dto.ApiError;
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
@Tag(name = "Modèles", description = "Gestion complète des modèles de rapports : création, publication, version, variables, génération de documents")
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
        description = """
            Crée un modèle de rapport en statut BROUILLON.
            Le code entreprise (header X-Entreprise-Code) détermine si le modèle est privé (associé à une entreprise) ou public (sans header).
            Le format papier peut être standard (A4, A5, etc.) ou personnalisé (CUSTOM avec largeurMm et hauteurMm).
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Modèle créé avec succès",
                     content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides ou contraintes de format non respectées",
                     content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "401", description = "Non authentifié (si authentification requise à l'avenir)",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<TemplateResponse> create(@Valid @RequestBody TemplateCreate request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.create(request));
    }

    @Operation(
        summary = "Lister tous les modèles accessibles",
        description = """
            Retourne les modèles visibles selon le contexte entreprise :
            - Sans header X-Entreprise-Code : uniquement les modèles publics (codeEntreprise null).
            - Avec un header : les modèles publics + les modèles privés de cette entreprise.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des modèles accessibles",
                     content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
        @ApiResponse(responseCode = "400", description = "Code entreprise invalide (mal formé)",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getAll() {
        return ResponseEntity.ok(templateService.findAll());
    }

    @Operation(
        summary = "Obtenir un modèle par ID",
        description = "Retourne les détails d'un modèle s'il est accessible (public ou privé avec le bon header)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Modèle trouvé",
                     content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable ou inaccessible",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> getById(
            @Parameter(description = "Identifiant UUID du modèle", required = true)
            @PathVariable @NonNull UUID id) {
        return ResponseEntity.ok(templateService.findById(id));
    }

    @Operation(
        summary = "Mettre à jour un modèle",
        description = """
            Met à jour les champs modifiables d'un modèle (nom, description, contenuDesign, format).
            Uniquement possible si le modèle est en statut BROUILLON.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Modèle mis à jour",
                     content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides ou état incompatible (non brouillon)",
                     content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable ou inaccessible",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> update(
            @Parameter(description = "Identifiant UUID du modèle", required = true)
            @PathVariable @NonNull UUID id,
            @Valid @RequestBody TemplateCreate request) {
        return ResponseEntity.ok(templateService.update(id, request));
    }

    @Operation(
        summary = "Supprimer un modèle",
        description = "Supprime définitivement un modèle, quel que soit son statut."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Modèle supprimé avec succès"),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable ou inaccessible",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identifiant UUID du modèle", required = true)
            @PathVariable @NonNull UUID id) {
        templateService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Publier un modèle",
        description = """
            Publie un modèle en BROUILLON : extrait automatiquement les variables du design,
            fige le schéma et passe le statut en PUBLIE. Le modèle ne sera plus modifiable.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Modèle publié avec succès",
                     content = @Content(schema = @Schema(implementation = TemplateResponse.class))),
        @ApiResponse(responseCode = "400", description = "Le modèle n'est pas en brouillon",
                     content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable ou inaccessible",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/publish")
    public ResponseEntity<TemplateResponse> publish(
            @Parameter(description = "Identifiant UUID du modèle", required = true)
            @PathVariable @NonNull UUID id) {
        return ResponseEntity.ok(templateService.publish(id));
    }

    // ---------- Variables ----------

    @Operation(
        summary = "Lister les variables d'un modèle",
        description = "Retourne les variables explicites définies pour un modèle donné."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des variables",
                     content = @Content(schema = @Schema(implementation = VariableResponse.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable ou inaccessible",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{templateId}/variables")
    public ResponseEntity<List<VariableResponse>> getVariables(
            @Parameter(description = "Identifiant UUID du modèle", required = true)
            @PathVariable @NonNull UUID templateId) {
        return ResponseEntity.ok(variableService.getVariables(templateId));
    }

    @Operation(
        summary = "Ajouter une variable à un modèle",
        description = "Ajoute une variable explicite au modèle (uniquement en statut BROUILLON)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Variable créée",
                     content = @Content(schema = @Schema(implementation = VariableResponse.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides ou doublon",
                     content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable ou inaccessible",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{templateId}/variables")
    public ResponseEntity<VariableResponse> addVariable(
            @Parameter(description = "Identifiant UUID du modèle", required = true)
            @PathVariable @NonNull UUID templateId,
            @Valid @RequestBody VariableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(variableService.addVariable(templateId, request));
    }

    @Operation(
        summary = "Supprimer une variable",
        description = "Supprime une variable existante (uniquement en statut BROUILLON)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Variable supprimée avec succès"),
        @ApiResponse(responseCode = "400", description = "Variable non supprimable (modèle non brouillon)",
                     content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Variable ou modèle introuvable",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{templateId}/variables/{variableId}")
    public ResponseEntity<Void> deleteVariable(
            @Parameter(description = "Identifiant UUID du modèle", required = true)
            @PathVariable @NonNull UUID templateId,
            @Parameter(description = "Identifiant UUID de la variable", required = true)
            @PathVariable @NonNull UUID variableId) {
        variableService.deleteVariable(templateId, variableId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Mettre à jour une variable",
        description = "Modifie les informations d'une variable existante (uniquement en statut BROUILLON)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Variable mise à jour",
                     content = @Content(schema = @Schema(implementation = VariableResponse.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides ou état incompatible",
                     content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Variable ou modèle introuvable",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{templateId}/variables/{variableId}")
    public ResponseEntity<VariableResponse> updateVariable(
            @Parameter(description = "Identifiant UUID du modèle", required = true)
            @PathVariable UUID templateId,
            @Parameter(description = "Identifiant UUID de la variable", required = true)
            @PathVariable UUID variableId,
            @Valid @RequestBody VariableRequest request) {
        return ResponseEntity.ok(variableService.updateVariable(templateId, variableId, request));
    }

    // ---------- Schema ----------

    @Operation(
        summary = "Obtenir le dictionnaire des variables (schéma)",
        description = "Retourne le schéma JSON des variables attendues par un modèle publié, avec type et obligation."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Schéma récupéré",
                     content = @Content(schema = @Schema(implementation = TemplateSchemaDto.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable ou inaccessible",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}/schema")
    public ResponseEntity<TemplateSchemaDto> schema(
            @Parameter(description = "Identifiant UUID du modèle", required = true)
            @PathVariable @NonNull UUID id) {
        return ResponseEntity.ok(schemaService.getSchema(id));
    }

    // ---------- Génération synchrone ----------

    @Operation(
        summary = "Générer un document PDF",
        description = """
            Reçoit les données réelles (variables du modèle), valide qu'elles correspondent au schéma,
            puis génère le PDF final. Le modèle doit être publié.
            Les données doivent être un objet JSON avec les clés correspondant aux variables du modèle.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "PDF généré avec succès",
                     content = @Content(mediaType = "application/pdf")),
        @ApiResponse(responseCode = "400", description = "Données invalides ou champ manquant",
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable ou non publié",
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/generate")
    public ResponseEntity<byte[]> generate(
            @Parameter(description = "Identifiant UUID du modèle publié", required = true)
            @PathVariable @NonNull UUID id,
            @org.springframework.web.bind.annotation.RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Données à injecter dans le modèle (exemple pour une facture)",
                content = @Content(
                    schema = @Schema(type = "object", example = """
                        {
                          "nom_client": "Entreprise ABC",
                          "date_emission": "2026-08-19",
                          "montant_total": 1250.50,
                          "lignes_commande": [
                            { "designation": "Produit A", "quantite": 2, "prix_unitaire": 100.00 },
                            { "designation": "Produit B", "quantite": 5, "prix_unitaire": 210.10 }
                          ]
                        }
                        """)
                )
            )
            // @org.springframework.web.bind.annotation.RequestBody
            Map<String, Object> data) {
        byte[] pdf = generationService.generateSync(id, data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rapport.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(
        summary = "Aperçu HTML d'un document",
        description = """
            Retourne le HTML généré à partir des données et du design, sans conversion PDF.
            Utile pour le débogage ou l'aperçu dans un navigateur.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "HTML généré",
                     content = @Content(mediaType = "text/html")),
        @ApiResponse(responseCode = "400", description = "Données invalides",
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable ou non publié",
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/preview-html")
    public ResponseEntity<String> previewHtml(
            @Parameter(description = "Identifiant UUID du modèle", required = true)
            @PathVariable @NonNull UUID id,
            @org.springframework.web.bind.annotation.RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Données à injecter (même format que pour la génération PDF)",
                content = @Content(
                    schema = @Schema(type = "object", example = """
                        {
                          "nom_client": "Entreprise ABC",
                          "date_emission": "2026-08-19"
                        }
                        """)
                )
            )
            // @org.springframework.web.bind.annotation.RequestBody
            Map<String, Object> data) {
        String html = generationService.generateHtml(id, data);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                .body(html);
    }

    // ---------- Génération asynchrone ----------

    @Operation(
        summary = "Générer un document PDF (asynchrone)",
        description = """
            Lance la génération en arrière-plan et retourne immédiatement un identifiant de tâche.
            Le PDF pourra être téléchargé ultérieurement via l'historique des générations.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Tâche de génération acceptée",
                     content = @Content(schema = @Schema(implementation = GenerationResponse.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides",
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable ou non publié",
                     content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/generate-async")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GenerationResponse generateAsync(
            @Parameter(description = "Identifiant UUID du modèle publié", required = true)
            @PathVariable @NonNull UUID id,
            @org.springframework.web.bind.annotation.RequestBody
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Données à injecter (même format que pour la génération synchrone)",
                content = @Content(
                    schema = @Schema(type = "object", example = """
                        {
                          "nom_client": "Entreprise ABC",
                          "date_emission": "2026-08-19"
                        }
                        """)
                )
            )
            // @org.springframework.web.bind.annotation.RequestBody
            Map<String, Object> data) {
        return generationService.generateAsync(id, data);
    }

    // ---------- Historique ----------

    @Operation(
        summary = "Historique des générations d'un modèle",
        description = "Retourne la liste des générations effectuées pour un modèle donné, triées de la plus récente à la plus ancienne."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des générations",
                     content = @Content(schema = @Schema(implementation = GenerationDto.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable ou inaccessible",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}/generations")
    public ResponseEntity<List<GenerationDto>> generations(
            @Parameter(description = "Identifiant UUID du modèle", required = true)
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
        @ApiResponse(responseCode = "404", description = "Modèle original introuvable ou inaccessible",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/duplicate")
    public ResponseEntity<TemplateResponse> duplicate(
            @Parameter(description = "Identifiant UUID du modèle à dupliquer", required = true)
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
                     content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable ou inaccessible",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/archive")
    public ResponseEntity<TemplateResponse> archive(
            @Parameter(description = "Identifiant UUID du modèle", required = true)
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
        @ApiResponse(responseCode = "404", description = "Modèle introuvable ou inaccessible",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/new-version")
    public ResponseEntity<TemplateResponse> newVersion(
            @Parameter(description = "Identifiant UUID du modèle publié ou archivé", required = true)
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
                     content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable ou inaccessible",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/restore")
    public ResponseEntity<TemplateResponse> restore(
            @Parameter(description = "Identifiant UUID du modèle archivé", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(templateService.restore(id));
    }
}