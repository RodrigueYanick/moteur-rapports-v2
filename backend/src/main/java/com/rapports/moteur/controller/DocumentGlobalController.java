package com.rapports.moteur.controller;

import com.rapports.moteur.dto.dtoDocument.DocumentResponse;
import com.rapports.moteur.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Tous les documents", description = "Consultation globale des documents générés, toutes modèles confondus pour l'entreprise courante")
public class DocumentGlobalController {

    private final DocumentService documentService;

    public DocumentGlobalController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(
        summary = "Lister tous les documents",
        description = "Retourne l'ensemble des documents générés pour l'entreprise associée au code entreprise fourni dans l'en-tête."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des documents",
                     content = @Content(schema = @Schema(implementation = List.class))),
        @ApiResponse(responseCode = "400", description = "Code entreprise manquant ou invalide",
                     content = @Content),
        @ApiResponse(responseCode = "404", description = "Aucune entreprise trouvée pour le code fourni",
                     content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAll());
    }
}