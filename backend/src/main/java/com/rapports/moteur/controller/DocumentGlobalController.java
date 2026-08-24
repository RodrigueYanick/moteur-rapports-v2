package com.rapports.moteur.controller;

import com.rapports.moteur.dto.ApiError;
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
@Tag(name = "Documents globaux", description = "Consultation de tous les documents générés, tout modèle confondu, pour l'entreprise courante")
public class DocumentGlobalController {

    private final DocumentService documentService;

    public DocumentGlobalController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(
        summary = "Lister tous les documents",
        description = """
            Retourne l'ensemble des documents générés pour l'entreprise associée au code entreprise fourni dans l'en-tête.
            
            - Sans header X-Entreprise-Code : retourne uniquement les documents liés à des modèles publics.
            - Avec header X-Entreprise-Code : retourne les documents des modèles publics + ceux des modèles privés de cette entreprise.
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des documents",
                     content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Code entreprise mal formé",
                     content = @Content(schema = @Schema(implementation = ApiError.class))),
        @ApiResponse(responseCode = "404", description = "Aucune entreprise trouvée pour le code fourni",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAll());
    }
}