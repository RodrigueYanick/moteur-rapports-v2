package com.rapports.moteur.controller;

import com.rapports.moteur.dto.CreateTemplateRequest;
import com.rapports.moteur.dto.ReportTemplateDTO;
import com.rapports.moteur.dto.UpdateTemplateRequest;
import com.rapports.moteur.service.ReportTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates")
public class ReportTemplateController {
    private final ReportTemplateService service;

    public ReportTemplateController(ReportTemplateService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReportTemplateDTO> list() {
        return service.findAll();
    }

    @PostMapping 
    @ResponseStatus(HttpStatus.CREATED)
    public ReportTemplateDTO create(@Valid @RequestBody CreateTemplateRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public ReportTemplateDTO get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public ReportTemplateDTO update(@PathVariable UUID id, @Valid @RequestBody UpdateTemplateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
