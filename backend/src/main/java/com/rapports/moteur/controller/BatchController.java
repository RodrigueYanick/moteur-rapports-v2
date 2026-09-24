package com.rapports.moteur.controller;

import com.rapports.moteur.dto.dtoBatch.*;
import com.rapports.moteur.service.BatchGenerationService;
import com.rapports.moteur.service.WebhookDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Génération par lot & Webhooks", description = "Gestion des traitements par lot asynchrones, webhooks de rappel et archives ZIP")
public class BatchController {

    private final BatchGenerationService batchService;
    private final WebhookDeliveryService webhookDeliveryService;

    @Operation(summary = "Lancer un nouveau lot de génération asynchrone")
    @PostMapping("/api/batches")
    public ResponseEntity<BatchResponse> createBatch(@Valid @RequestBody BatchCreateRequest request) {
        BatchResponse response = batchService.createAndStartBatch(request.getTemplateId(), request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(summary = "Lancer un lot de génération pour un modèle spécifique")
    @PostMapping("/api/templates/{templateId}/batch")
    public ResponseEntity<BatchResponse> createBatchForTemplate(
            @PathVariable UUID templateId,
            @Valid @RequestBody BatchCreateRequest request) {
        BatchResponse response = batchService.createAndStartBatch(templateId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @Operation(summary = "Lister les lots de l'entreprise ou d'un modèle")
    @GetMapping("/api/batches")
    public ResponseEntity<List<BatchResponse>> listBatches(
            @Parameter(description = "Filtrer par modèle (optionnel)")
            @RequestParam(required = false) UUID templateId) {
        return ResponseEntity.ok(batchService.listBatches(templateId));
    }

    @Operation(summary = "Consulter l'avancement et les métriques d'un lot")
    @GetMapping("/api/batches/{id}")
    public ResponseEntity<BatchResponse> getBatch(@PathVariable UUID id) {
        return ResponseEntity.ok(batchService.getBatch(id));
    }

    @Operation(summary = "Lister les éléments détaillés d'un lot")
    @GetMapping("/api/batches/{id}/items")
    public ResponseEntity<List<BatchItemResponse>> getBatchItems(@PathVariable UUID id) {
        return ResponseEntity.ok(batchService.getBatchItems(id));
    }

    @Operation(summary = "Relancer les éléments d'un lot ayant échoué")
    @PostMapping("/api/batches/{id}/retry-failed")
    public ResponseEntity<BatchResponse> retryFailed(@PathVariable UUID id) {
        return ResponseEntity.ok(batchService.retryFailedItems(id));
    }

    @Operation(summary = "Télécharger l'ensemble des rapports générés sous forme d'archive ZIP")
    @GetMapping("/api/batches/{id}/download-zip")
    public ResponseEntity<byte[]> downloadZip(@PathVariable UUID id) {
        byte[] zipBytes = batchService.generateZipArchive(id);
        String filename = "batch_" + id + ".zip";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(zipBytes.length))
                .body(zipBytes);
    }

    @Operation(summary = "Tester la connectivité et la signature d'un webhook distant")
    @PostMapping("/api/batches/webhooks/test")
    public ResponseEntity<WebhookTestResponse> testWebhook(@Valid @RequestBody WebhookTestRequest request) {
        WebhookTestResponse response = webhookDeliveryService.pingWebhook(request.getUrl(), request.getSecret());
        return ResponseEntity.ok(response);
    }
}

