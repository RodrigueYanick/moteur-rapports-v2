package com.rapports.moteur.controller;

import com.rapports.moteur.dto.dtoDocument.DocumentResponse;
import com.rapports.moteur.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentGlobalController {

    private final DocumentService documentService;

    public DocumentGlobalController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAll());
    }
}