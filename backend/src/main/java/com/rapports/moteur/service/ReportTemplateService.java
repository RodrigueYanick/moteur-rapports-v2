package com.rapports.moteur.service;

import com.rapports.moteur.dto.CreateTemplateRequest;
import com.rapports.moteur.dto.ReportTemplateDTO;
import com.rapports.moteur.dto.ReportVariableDTO;
import com.rapports.moteur.dto.UpdateTemplateRequest;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.entity.ReportVariable;
import com.rapports.moteur.entity.TemplateStatus;
import com.rapports.moteur.repository.ReportTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ReportTemplateService {
    private final ReportTemplateRepository repository;

    public ReportTemplateService(ReportTemplateRepository repository) {
        this.repository = repository;
    }

    public List<ReportTemplateDTO> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public ReportTemplateDTO create(CreateTemplateRequest request) {
        ReportTemplate template = new ReportTemplate();
        template.setName(request.name());
        template.setDescription(request.description());
        template.setStatus(TemplateStatus.ACTIVE);

        List<ReportVariable> variables = request.variables().stream().map(this::toEntity).toList();
        variables.forEach(v -> v.setTemplate(template));
        template.setVariables(variables.stream().toList());
        return toDto(repository.save(template));
    }

    public ReportTemplateDTO findById(UUID id) {
        return toDto(repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Template not found")));
    }

    public ReportTemplateDTO update(UUID id, UpdateTemplateRequest request) {
        ReportTemplate template = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Template not found"));
        template.setName(request.name());
        template.setDescription(request.description());
        return toDto(repository.save(template));
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }

    private ReportTemplateDTO toDto(ReportTemplate template) {
        return new ReportTemplateDTO(
                template.getId(),
                template.getName(),
                template.getDescription(),
                template.getStatus(),
                template.getVariables().stream().map(this::toDto).toList(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }

    private ReportVariableDTO toDto(ReportVariable variable) {
        return new ReportVariableDTO(
                variable.getId(),
                variable.getName(),
                variable.getType(),
                variable.getDefaultValue(),
                variable.isRequired()
        );
    }

    private ReportVariable toEntity(ReportVariableDTO dto) {
        ReportVariable variable = new ReportVariable();
        variable.setName(dto.name());
        variable.setType(dto.type());
        variable.setDefaultValue(dto.defaultValue());
        variable.setRequired(dto.required());
        return variable;
    }
}
