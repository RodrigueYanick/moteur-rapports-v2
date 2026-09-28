package com.rapports.moteur.controller;

import com.rapports.moteur.dto.datasource.*;
import com.rapports.moteur.service.DataSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/data-sources")
@RequiredArgsConstructor
@Tag(name = "Sources de Données Distantes", description = "Gestion et exécution des connecteurs de données externes (PostgreSQL, MySQL, APIs REST)")
public class DataSourceController {

    private final DataSourceService dataSourceService;

    @Operation(summary = "Lister toutes les sources de données de l'entreprise")
    @GetMapping
    public ResponseEntity<List<DataSourceResponse>> listDataSources() {
        return ResponseEntity.ok(dataSourceService.listDataSources());
    }

    @Operation(summary = "Récupérer le détail d'une source de données")
    @GetMapping("/{id}")
    public ResponseEntity<DataSourceResponse> getDataSource(@PathVariable UUID id) {
        return ResponseEntity.ok(dataSourceService.getDataSource(id));
    }

    @Operation(summary = "Créer une nouvelle source de données distante")
    @PostMapping
    public ResponseEntity<DataSourceResponse> createDataSource(@Valid @RequestBody DataSourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dataSourceService.createDataSource(request));
    }

    @Operation(summary = "Mettre à jour une source de données")
    @PutMapping("/{id}")
    public ResponseEntity<DataSourceResponse> updateDataSource(
            @PathVariable UUID id,
            @Valid @RequestBody DataSourceRequest request
    ) {
        return ResponseEntity.ok(dataSourceService.updateDataSource(id, request));
    }

    @Operation(summary = "Supprimer une source de données")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDataSource(@PathVariable UUID id) {
        dataSourceService.deleteDataSource(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Tester la connectivité vers une source (existante ou en cours de saisie)")
    @PostMapping("/test")
    public ResponseEntity<DataSourceTestResult> testConnection(@RequestBody DataSourceTestRequest request) {
        return ResponseEntity.ok(dataSourceService.testConnection(request));
    }

    @Operation(summary = "Tester la connectivité vers une source existante par son ID")
    @PostMapping("/{id}/test")
    public ResponseEntity<DataSourceTestResult> testExistingConnection(
            @PathVariable UUID id,
            @RequestParam(required = false) String testQuery
    ) {
        DataSourceTestRequest req = DataSourceTestRequest.builder()
                .dataSourceId(id)
                .testQuery(testQuery)
                .build();
        return ResponseEntity.ok(dataSourceService.testConnection(req));
    }

    @Operation(summary = "Exécuter une requête ou un appel API sur une source distante pour prévisualiser les données")
    @PostMapping("/{id}/execute")
    public ResponseEntity<Object> executeQuery(
            @PathVariable UUID id,
            @Valid @RequestBody DataSourceExecuteRequest request
    ) {
        return ResponseEntity.ok(dataSourceService.executeQuery(id, request));
    }
}
