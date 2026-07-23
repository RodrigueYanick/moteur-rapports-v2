package com.rapports.moteur.service;

import com.rapports.moteur.dto.dtoTemplate.TemplateSchemaDto;
import com.rapports.moteur.dto.dtoVariable.VariableResponse;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.exceptions.TemplateNotFoundException;
import com.rapports.moteur.mapper.VariableMapper;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.repository.ReportVariableRepository;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

@Service
public class SchemaService {

    private final ReportTemplateRepository templateRepository;
    private final ReportVariableRepository variableRepository;
    private final VariableMapper variableMapper;

    public SchemaService(ReportTemplateRepository templateRepository,
                          ReportVariableRepository variableRepository,
                          VariableMapper variableMapper) {
        this.templateRepository = templateRepository;
        this.variableRepository = variableRepository;
        this.variableMapper = variableMapper;
    }

        public TemplateSchemaDto getSchema(@NonNull UUID templateId) {
                ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + templateId));

        List<VariableResponse> variables = variableRepository.findByTemplate_Id(templateId)
                .stream().map(variableMapper::toDto).toList();

        return TemplateSchemaDto.builder()
                .templateId(template.getId())
                .nom(template.getNom())
                .version(template.getVersion())
                .variables(variables)
                .build();
    }
}