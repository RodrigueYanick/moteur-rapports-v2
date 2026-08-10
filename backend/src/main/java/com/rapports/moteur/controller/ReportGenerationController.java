package com.rapports.moteur.controller;

import com.rapports.moteur.dto.dtoDocument.DocumentResponse;
import com.rapports.moteur.dto.dtoGeneration.GenerationDto;
import com.rapports.moteur.service.DocumentService;
import com.rapports.moteur.service.ReportGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/generations")
public class ReportGenerationController {

    @Autowired
    private ReportGenerationService service;

    @Autowired
    private DocumentService documentService;

    @GetMapping
    public List<GenerationDto> getGenerationsForTemplate(@RequestParam @NonNull UUID templateId) {
        return service.getHistory(templateId);
    }

    @GetMapping("/api/documents")
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAll());
    }
}
