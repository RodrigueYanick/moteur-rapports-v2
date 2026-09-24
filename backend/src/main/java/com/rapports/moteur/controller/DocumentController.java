package com.rapports.moteur.controller;

import com.rapports.moteur.dto.ApiError;
import com.rapports.moteur.dto.dtoDocument.DocumentCreate;
import com.rapports.moteur.dto.dtoDocument.DocumentResponse;
import com.rapports.moteur.dto.dtoDocument.DocumentEmailRequest;
import com.rapports.moteur.dto.dtoDocument.DocumentEmailResponse;
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

import org.springframework.http.HttpHeaders;
import java.util.List;
import java.util.UUID;
import com.rapports.moteur.service.ReportGenerationService;

@RestController
@RequestMapping("/api/templates/{templateId}/documents")
@RequiredArgsConstructor
@Tag(name = "Documents d'un modèle", description = "Gestion des documents générés pour un modèle spécifique")
public class DocumentController {

    private final DocumentService documentService;
    private final ReportGenerationService generationService;

    @Operation(
        summary = "Créer un document",
        description = """
            Enregistre un nouveau document pour le modèle spécifié.
            Le document est créé avec le statut BROUILLON.
            Les données fournies doivent correspondre aux variables du modèle.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Document créé avec succès",
                     content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides ou erreur de validation",
                     content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable ou inaccessible",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<DocumentResponse> create(
            @Parameter(description = "Identifiant UUID du modèle", required = true,
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID templateId,
            @Valid @RequestBody DocumentCreate request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(documentService.create(templateId, request));
    }

    @Operation(
        summary = "Lister les documents d'un modèle",
        description = "Retourne tous les documents enregistrés pour le modèle spécifié."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des documents",
                     content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Modèle introuvable ou inaccessible",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getByTemplate(
            @Parameter(description = "Identifiant UUID du modèle", required = true,
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID templateId) {
        return ResponseEntity.ok(documentService.getByTemplate(templateId));
    }

    @Operation(
        summary = "Obtenir un document par ID",
        description = "Retourne les détails d'un document spécifique appartenant au template donné."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Document trouvé",
                     content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
        @ApiResponse(responseCode = "404", description = "Document introuvable ou inaccessible",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getById(
            @Parameter(description = "Identifiant UUID du template", required = true)
            @PathVariable UUID templateId,
            @Parameter(description = "Identifiant UUID du document", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(documentService.getById(templateId, id));
    }

    @Operation(
        summary = "Mettre à jour un document",
        description = "Met à jour les données d'un document existant appartenant au template donné."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Document mis à jour",
                     content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides",
                     content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Document introuvable ou inaccessible",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<DocumentResponse> update(
            @Parameter(description = "Identifiant UUID du template", required = true)
            @PathVariable UUID templateId,
            @Parameter(description = "Identifiant UUID du document", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody DocumentCreate request) {
        return ResponseEntity.ok(documentService.update(templateId, id, request));
    }

    @Operation(
        summary = "Supprimer un document",
        description = "Supprime définitivement un document existant appartenant au template donné."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Document supprimé avec succès"),
        @ApiResponse(responseCode = "404", description = "Document introuvable ou inaccessible",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identifiant UUID du template", required = true)
            @PathVariable UUID templateId,
            @Parameter(description = "Identifiant UUID du document", required = true)
            @PathVariable UUID id) {
        documentService.delete(templateId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Exporter un document en Excel (.xlsx)",
        description = "Génère un classeur Excel (.xlsx) à partir des données enregistrées du document."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Fichier Excel généré avec succès",
                     content = @Content(mediaType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
        @ApiResponse(responseCode = "404", description = "Document ou modèle introuvable",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping("/{id}/export-excel")
    public ResponseEntity<byte[]> exportExcel(
            @Parameter(description = "Identifiant UUID du template", required = true)
            @PathVariable UUID templateId,
            @Parameter(description = "Identifiant UUID du document", required = true)
            @PathVariable UUID id) {
        DocumentResponse doc = documentService.getById(templateId, id);
        byte[] excel = generationService.generateExcel(templateId, doc.getDonnees());
        String cleanName = doc.getNom() != null ? doc.getNom().replaceAll("[^a-zA-Z0-9._-]", "_") : "document";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + cleanName + ".xlsx\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excel);
    }

    @Operation(
        summary = "Envoyer un document par email",
        description = "Expédie le document sous forme de pièce jointe PDF à l'adresse email spécifiée."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email expédié avec succès",
                     content = @Content(schema = @Schema(implementation = DocumentEmailResponse.class))),
        @ApiResponse(responseCode = "400", description = "Données d'email invalides",
                     content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Document ou modèle introuvable",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/{id}/send-email")
    public ResponseEntity<DocumentEmailResponse> sendEmail(
            @Parameter(description = "Identifiant UUID du template", required = true)
            @PathVariable UUID templateId,
            @Parameter(description = "Identifiant UUID du document", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody DocumentEmailRequest request) {
        return ResponseEntity.ok(documentService.sendEmail(templateId, id, request));
    }
}