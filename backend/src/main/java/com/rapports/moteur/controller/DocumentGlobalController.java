package com.rapports.moteur.controller;

import com.rapports.moteur.dto.ApiError;
import com.rapports.moteur.dto.dtoDocument.DocumentResponse;
import com.rapports.moteur.entity.Visibilite;
import com.rapports.moteur.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents globaux", description = "Consultation de tous les documents générés, tout modèle confondu, pour l'entreprise courante")
public class DocumentGlobalController {

    private final DocumentService documentService;

    public DocumentGlobalController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(
        summary = "Lister tous les documents selon la visibilité",
        description = """
            Retourne les documents accessibles en fonction du filtre de visibilité et de la recherche.
            Paramètres optionnels :
            - visibilite : ALL (défaut si header présent), PRIVATE (uniquement documents privés), PUBLIC (uniquement documents publics)
            - q : terme de recherche sur le nom du document
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des documents",
                    content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Paramètres invalides",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments(
            @Parameter(description = "Filtre de visibilité (ALL, PRIVATE, PUBLIC)")
            @RequestParam(required = false) Visibilite visibilite,
            @Parameter(description = "Terme de recherche sur le nom du document")
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(documentService.getAll(visibilite, q));
    }
}