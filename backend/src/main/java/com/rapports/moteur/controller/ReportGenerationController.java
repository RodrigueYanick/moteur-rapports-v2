package com.rapports.moteur.controller;

import com.rapports.moteur.dto.GenerateReportRequest;
import com.rapports.moteur.dto.ReportGenerationDTO;
import com.rapports.moteur.service.ReportGenerationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/generations")
public class ReportGenerationController {
    private final ReportGenerationService service;

    public ReportGenerationController(ReportGenerationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReportGenerationDTO> list() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportGenerationDTO create(@Valid @RequestBody GenerateReportRequest request) {
        return service.generate(request);
    }

    @GetMapping("/{id}")
    public ReportGenerationDTO get(@PathVariable UUID id) {
        return service.findById(id);
    }
}
