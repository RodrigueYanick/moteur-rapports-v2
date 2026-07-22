package com.rapports.moteur.service;

import com.rapports.moteur.dto.dtoTemplate.TemplateCreate;
import com.rapports.moteur.dto.dtoTemplate.TemplateResponse;
import com.rapports.moteur.dto.dtoVariable.VariableRequest;
import com.rapports.moteur.dto.dtoVariable.VariableResponse;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.entity.ReportVariable;
import com.rapports.moteur.entity.TemplateStatus;
import com.rapports.moteur.mapper.TemplateMapper;
import com.rapports.moteur.mapper.VariableMapper;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.repository.ReportVariableRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportTemplateService {

    @Autowired
    private VariableMapper variableMapper;

    @Autowired
    private ReportTemplateRepository repository;

    @Autowired
    private ReportVariableRepository variableRepository;

    @Autowired
    private TemplateMapper mapper;

    public List<TemplateResponse> findAll() {
        List<ReportTemplate> templates = repository.findAll();
        List<TemplateResponse> templateDtos = new ArrayList<>();
        for (ReportTemplate template: templates){
            templateDtos.add(mapper.toDto(template));
        }
        return templateDtos;
    }

    public TemplateResponse create(TemplateCreate request) {
        ReportTemplate template = mapper.toEntity(request);
        template.setNom(request.getNom());
        template.setDescription(request.getDescription());
        template.setStatut(TemplateStatus.PUBLIE);
        ReportTemplate saved = repository.save(template);
        return mapper.toDto(saved);
    }

    public TemplateResponse findById(UUID id) {
        Optional<ReportTemplate> template = repository.findById(id);
        if (template.isPresent()) {
            return mapper.toDto(template.get());
        }
        return null;
    }

    public void delete(UUID id) {
        repository.deleteById(id);
    }

    public List<VariableResponse> findVariables(UUID templateId) {
        List<ReportVariable> variables = variableRepository.findByTemplate_Id(templateId);
        List<VariableResponse> variableResponses = new ArrayList<>();
        for (ReportVariable variable : variables) {
            variableResponses.add(variableMapper.toDto(variable));
        }
        return variableResponses;
    }

    public VariableResponse addVariable(UUID templateId, VariableRequest request) {
        ReportTemplate template = repository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Template not found"));

        ReportVariable variable = new ReportVariable();
        variable.setTemplate(template);
        variable.setNomVariable(request.getNomVariable());
        variable.setType(request.getType());
        variable.setObligatoire(request.getObligatoire() != null ? request.getObligatoire() : false);

        return variableMapper.toDto(variableRepository.save(variable));
    }

    public void deleteVariable(UUID templateId, UUID variableId) {
        ReportVariable variable = variableRepository.findById(variableId)
                .orElseThrow(() -> new IllegalArgumentException("Variable not found"));

        if (!variable.getTemplate().getId().equals(templateId)) {
            throw new IllegalArgumentException("Variable does not belong to the requested template");
        }

        variableRepository.delete(variable);
    }


}
