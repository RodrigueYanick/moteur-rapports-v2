package com.rapports.moteur.controller;

import com.rapports.moteur.dto.dtoTemplate.TemplateCreate;
import com.rapports.moteur.dto.dtoTemplate.TemplateResponse;
import com.rapports.moteur.dto.dtoVariable.VariableRequest;
import com.rapports.moteur.dto.dtoVariable.VariableResponse;
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

    @GetMapping("/all")
    public List<TemplateResponse> list() {
        return service.findAll();
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateResponse create(@Valid @RequestBody TemplateCreate request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public TemplateResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/variables")
    public List<VariableResponse> getVariables(@PathVariable UUID id) {
        return service.findVariables(id);
    }

    @PostMapping("/{id}/variables")
    @ResponseStatus(HttpStatus.CREATED)
    public VariableResponse addVariable(@PathVariable UUID id, @Valid @RequestBody VariableRequest request) {
        return service.addVariable(id, request);
    }

    @DeleteMapping("/{id}/variables/{variableId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVariable(@PathVariable UUID id, @PathVariable UUID variableId) {
        service.deleteVariable(id, variableId);
    }

    // @PutMapping("/{id}")
    // public TemplateResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateTemplateRequest request) {
    //     return service.update(id, request);
    // }

    // @DeleteMapping("/{id}")
    // @ResponseStatus(HttpStatus.NO_CONTENT)
    // public void delete(@PathVariable UUID id) {
    //     service.delete(id);
    // }
}
