package com.rapports.moteur.controller;

import com.rapports.moteur.dto.ApiError;
import com.rapports.moteur.dto.schedule.ScheduledJobExecutionResponse;
import com.rapports.moteur.dto.schedule.ScheduledJobRequest;
import com.rapports.moteur.dto.schedule.ScheduledJobResponse;
import com.rapports.moteur.service.ScheduledJobService;
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
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@Tag(name = "Planificateur de Rapports", description = "Gestion des automatisations, expressions CRON et diffusions récurrentes")
public class ScheduledJobController {

    private final ScheduledJobService scheduledJobService;

    @Operation(summary = "Créer une planification récurrente", description = "Définit une tâche automatique selon une expression CRON avec envoi d'emails et webhook.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Planification créée avec succès",
                    content = @Content(schema = @Schema(implementation = ScheduledJobResponse.class))),
            @ApiResponse(responseCode = "400", description = "Expression CRON invalide ou modèle non trouvé",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping
    public ResponseEntity<ScheduledJobResponse> createJob(@Valid @RequestBody ScheduledJobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduledJobService.createJob(request));
    }

    @Operation(summary = "Modifier une planification", description = "Met à jour l'expression CRON, les paramètres ou les destinataires d'une planification existante.")
    @PutMapping("/{id}")
    public ResponseEntity<ScheduledJobResponse> updateJob(
            @Parameter(description = "Identifiant UUID de la planification", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody ScheduledJobRequest request) {
        return ResponseEntity.ok(scheduledJobService.updateJob(id, request));
    }

    @Operation(summary = "Supprimer une planification", description = "Supprime définitivement une tâche planifiée.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJob(
            @Parameter(description = "Identifiant UUID de la planification", required = true)
            @PathVariable UUID id) {
        scheduledJobService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Obtenir les détails d'une planification", description = "Retourne la configuration et la prochaine date d'exécution.")
    @GetMapping("/{id}")
    public ResponseEntity<ScheduledJobResponse> getJob(
            @Parameter(description = "Identifiant UUID de la planification", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(scheduledJobService.getJob(id));
    }

    @Operation(summary = "Lister les planifications de l'entreprise", description = "Retourne toutes les tâches planifiées de l'entreprise connectée.")
    @GetMapping
    public ResponseEntity<List<ScheduledJobResponse>> listJobs() {
        return ResponseEntity.ok(scheduledJobService.listJobs());
    }

    @Operation(summary = "Lister les planifications d'un modèle spécifique")
    @GetMapping("/template/{templateId}")
    public ResponseEntity<List<ScheduledJobResponse>> listJobsByTemplate(
            @Parameter(description = "Identifiant UUID du modèle", required = true)
            @PathVariable UUID templateId) {
        return ResponseEntity.ok(scheduledJobService.listJobsByTemplate(templateId));
    }

    @Operation(summary = "Déclencher l'exécution immédiate", description = "Exécute la tâche planifiée immédiatement sans attendre la prochaine échéance CRON.")
    @PostMapping("/{id}/run-now")
    public ResponseEntity<ScheduledJobExecutionResponse> runNow(
            @Parameter(description = "Identifiant UUID de la planification", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(scheduledJobService.runNow(id));
    }

    @Operation(summary = "Historique d'exécution d'une planification", description = "Retourne le journal des exécutions passées avec statut, durée et nombre de destinataires notifiés.")
    @GetMapping("/{id}/executions")
    public ResponseEntity<List<ScheduledJobExecutionResponse>> getJobExecutions(
            @Parameter(description = "Identifiant UUID de la planification", required = true)
            @PathVariable UUID id) {
        return ResponseEntity.ok(scheduledJobService.getJobExecutions(id));
    }
}
