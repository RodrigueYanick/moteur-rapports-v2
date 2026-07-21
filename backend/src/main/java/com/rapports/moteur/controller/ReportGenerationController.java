package com.rapports.moteur.controller;

import com.rapports.moteur.dto.dtoGeneration.GenerateRequest;
import com.rapports.moteur.dto.dtoGeneration.GenerationResponse;
import com.rapports.moteur.service.ReportGenerationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/generations")
public class ReportGenerationController {
    private final ReportGenerationService service;

    public ReportGenerationController(ReportGenerationService service) {
        this.service = service;
    }

    @GetMapping
    public List<GenerationResponse> list() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GenerationResponse create(@Valid @RequestBody GenerateRequest request) {
        return service.generate(request);
    }

    @GetMapping("/{id}")
    public GenerationResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }
}
