package com.rapports.moteur.controller;

import com.rapports.moteur.dto.dtoDocument.DocumentCreate;
import com.rapports.moteur.dto.dtoDocument.DocumentResponse;
import com.rapports.moteur.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates/{templateId}/documents")
@RequiredArgsConstructor
@Tag(name = "Documents d'un modèle", description = "Gestion des documents générés à partir d'un template spécifique")
public class DocumentController {

    private final DocumentService documentService;

    @Operation(
        summary = "Créer un document",
        description = "Enregistre un nouveau document pour le template donné, avec les données fournies."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Document créé avec succès",
                     content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides ou erreur de validation",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Template introuvable",
                     content = @Content)
    })
    @PostMapping
    public ResponseEntity<DocumentResponse> create(
            @Parameter(description = "Identifiant du template", required = true)
            @PathVariable UUID templateId,
            @Valid @RequestBody DocumentCreate request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.create(templateId, request));
    }

    @Operation(
        summary = "Lister les documents d'un template",
        description = "Retourne tous les documents enregistrés pour le template spécifié."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des documents",
                     content = @Content(schema = @Schema(implementation = List.class))),
        @ApiResponse(responseCode = "404", description = "Template introuvable",
                     content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getByTemplate(
            @Parameter(description = "Identifiant du template", required = true)
            @PathVariable UUID templateId) {
        return ResponseEntity.ok(documentService.getByTemplate(templateId));
    }

    @Operation(
        summary = "Obtenir un document par ID",
        description = "Retourne les détails d'un document spécifique."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Document trouvé",
                     content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Document introuvable",
                     content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getById(
            @Parameter(description = "Identifiant du document", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(documentService.getById(id));
    }

    @Operation(
        summary = "Mettre à jour un document",
        description = "Met à jour les données d'un document existant."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Document mis à jour",
                     content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Document introuvable",
                     content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> update(
            @Parameter(description = "Identifiant du document", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody DocumentCreate request) {
        return ResponseEntity.ok(documentService.update(id, request));
    }

    @Operation(
        summary = "Supprimer un document",
        description = "Supprime définitivement un document existant."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Document supprimé avec succès"),
        @ApiResponse(responseCode = "404", description = "Document introuvable",
                     content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identifiant du document", required = true)
            @PathVariable UUID id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}