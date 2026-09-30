package com.rapports.moteur.controller;

import com.rapports.moteur.dto.dtoTemplate.TemplateResponse;
import com.rapports.moteur.dto.dtoWorkflow.WorkflowActionRequest;
import com.rapports.moteur.dto.dtoWorkflow.WorkflowHistoryResponse;
import com.rapports.moteur.service.TemplateWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates/{id}/workflow")
@RequiredArgsConstructor
@Tag(name = "Workflow de Validation des Modèles", description = "Gestion du cycle de vie des modèles : soumission, approbation, rejet et publication")
public class TemplateWorkflowController {

    private final TemplateWorkflowService workflowService;

    @Operation(summary = "Soumettre un modèle pour revue", description = "Fait passer le modèle du statut BROUILLON à EN_REVUE")
    @PostMapping("/submit")
    public ResponseEntity<TemplateResponse> submitForReview(
            @PathVariable UUID id,
            @RequestBody(required = false) WorkflowActionRequest request) {
        return ResponseEntity.ok(workflowService.submitForReview(id, request));
    }

    @Operation(summary = "Approuver un modèle", description = "Fait passer le modèle du statut EN_REVUE à APPROUVE (réservé aux administrateurs)")
    @PostMapping("/approve")
    public ResponseEntity<TemplateResponse> approve(
            @PathVariable UUID id,
            @RequestBody(required = false) WorkflowActionRequest request) {
        return ResponseEntity.ok(workflowService.approveTemplate(id, request));
    }

    @Operation(summary = "Rejeter un modèle", description = "Fait repasser le modèle du statut EN_REVUE à BROUILLON avec un motif obligatoire (réservé aux administrateurs)")
    @PostMapping("/reject")
    public ResponseEntity<TemplateResponse> reject(
            @PathVariable UUID id,
            @RequestBody WorkflowActionRequest request) {
        return ResponseEntity.ok(workflowService.rejectTemplate(id, request));
    }

    @Operation(summary = "Publier un modèle en production", description = "Fait passer le modèle au statut PUBLIE (réservé aux administrateurs)")
    @PostMapping("/publish")
    public ResponseEntity<TemplateResponse> publish(
            @PathVariable UUID id,
            @RequestBody(required = false) WorkflowActionRequest request) {
        return ResponseEntity.ok(workflowService.publishTemplate(id, request));
    }

    @Operation(summary = "Consulter l'historique du workflow", description = "Retourne la liste chronologique des actions et commentaires sur le modèle")
    @GetMapping("/history")
    public ResponseEntity<List<WorkflowHistoryResponse>> getHistory(@PathVariable UUID id) {
        return ResponseEntity.ok(workflowService.getWorkflowHistory(id));
    }
}