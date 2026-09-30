package com.rapports.moteur.controller;

import com.rapports.moteur.dto.dtoAudit.AuditLogResponse;
import com.rapports.moteur.dto.dtoAudit.AuditStatsResponse;
import com.rapports.moteur.service.audit.AuditTrailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Piste d'Audit (Audit Trail)", description = "Tableau de bord et traçabilité complète des actions : qui a modifié quel modèle, et qui a généré ou téléchargé quel document avec IP et horodatage")
public class AuditController {

    private final AuditTrailService auditTrailService;

    @Operation(summary = "Recherche paginée dans la piste d'audit", description = "Filtre par action, type de ressource, email utilisateur et plage de dates")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_ENTREPRISE', 'SUPER_ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> searchAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String ressourceType,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        return ResponseEntity.ok(auditTrailService.searchLogs(action, ressourceType, userEmail, dateDebut, dateFin, pageable));
    }

    @Operation(summary = "Statistiques globales d'audit pour le tableau de bord", description = "Fournit le total d'événements et la répartition par type d'action")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN_ENTREPRISE', 'SUPER_ADMIN')")
    public ResponseEntity<AuditStatsResponse> getStats() {
        return ResponseEntity.ok(auditTrailService.getStats());
    }
}