package com.rapports.moteur.controller;

import com.rapports.moteur.dto.dtoGeneration.GenerationDto;
import com.rapports.moteur.dto.dtoGeneration.GenerationResponse;
import com.rapports.moteur.dto.dtoTemplate.TemplateCreate;
import com.rapports.moteur.dto.dtoTemplate.TemplateResponse;
import com.rapports.moteur.dto.dtoTemplate.TemplateSchemaDto;
import com.rapports.moteur.dto.dtoVariable.VariableRequest;
import com.rapports.moteur.dto.dtoVariable.VariableResponse;
import com.rapports.moteur.service.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.lang.NonNull;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    // Les services sont injectés via @Autowired (ou par constructeur, peu importe)
    private final ReportTemplateService templateService;
    private final VariableService variableService;
    private final SchemaService schemaService;
    private final ReportGenerationService generationService;

    public TemplateController(ReportTemplateService templateService,
                              VariableService variableService,
                              SchemaService schemaService,
                              ReportGenerationService generationService) {
        this.templateService = templateService;
        this.variableService = variableService;
        this.schemaService = schemaService;
        this.generationService = generationService;
    }

    // ---------- CRUD template ----------
    @PostMapping
    public ResponseEntity<TemplateResponse> create(@Valid @RequestBody TemplateCreate request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getAll() {
        return ResponseEntity.ok(templateService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> getById(@PathVariable @NonNull UUID id) {
        return ResponseEntity.ok(templateService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> update(@PathVariable @NonNull UUID id,
                                                   @Valid @RequestBody TemplateCreate request) {
        return ResponseEntity.ok(templateService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @NonNull UUID id) {
        templateService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<TemplateResponse> publish(@PathVariable @NonNull UUID id) {
        return ResponseEntity.ok(templateService.publish(id));
    }

    // ---------- Variables ----------
    @GetMapping("/{templateId}/variables")
    public ResponseEntity<List<VariableResponse>> getVariables(@PathVariable @NonNull UUID templateId) {
        return ResponseEntity.ok(variableService.getVariables(templateId));
    }

    @PostMapping("/{templateId}/variables")
    public ResponseEntity<VariableResponse> addVariable(@PathVariable @NonNull UUID templateId,
                                                        @Valid @RequestBody VariableRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(variableService.addVariable(templateId, request));
    }

    @DeleteMapping("/{templateId}/variables/{variableId}")
    public ResponseEntity<Void> deleteVariable(@PathVariable @NonNull UUID templateId,
                                               @PathVariable @NonNull UUID variableId) {
        variableService.deleteVariable(templateId, variableId);
        return ResponseEntity.noContent().build();
    }

    // ---------- Schema ----------
    @GetMapping("/{id}/schema")
    public ResponseEntity<TemplateSchemaDto> schema(@PathVariable @NonNull UUID id) {
        return ResponseEntity.ok(schemaService.getSchema(id));
    }

    // ---------- Génération synchrone ----------
    @PostMapping("/{id}/generate")
    public ResponseEntity<byte[]> generate(@PathVariable @NonNull UUID id,
                                           @RequestBody Map<String, Object> data) {
        byte[] pdf = generationService.generateSync(id, data);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rapport.pdf\"")
            .contentType(java.util.Objects.requireNonNull(MediaType.APPLICATION_PDF))
            .body(pdf);
    }

    // ---------- Génération asynchrone ----------
    @PostMapping("/{id}/generate-async")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GenerationResponse generateAsync(@PathVariable @NonNull UUID id,
                                            @RequestBody Map<String, Object> data) {
        return generationService.generateAsync(id, data);
    }

    // ---------- Historique ----------
    @GetMapping("/{id}/generations")
    public ResponseEntity<List<GenerationDto>> generations(@PathVariable @NonNull UUID id) {
        return ResponseEntity.ok(generationService.getHistory(id));
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<TemplateResponse> duplicate(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(templateService.duplicate(id));
    }
}