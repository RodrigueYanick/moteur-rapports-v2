package com.rapports.moteur.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.dtoTemplate.TemplateSchemaDto;
import com.rapports.moteur.dto.dtoVariable.VariableResponse;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.entity.VariableType;
import com.rapports.moteur.exceptions.TemplateNotFoundException;
import com.rapports.moteur.repository.ReportTemplateRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SchemaService {

    private final ReportTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;
    private final EntrepriseService entrepriseService;

    public SchemaService(ReportTemplateRepository templateRepository, ObjectMapper objectMapper, EntrepriseService entrepriseService) {
        this.templateRepository = templateRepository;
        this.objectMapper = objectMapper;
        this.entrepriseService = entrepriseService;
    }

    private ReportTemplate loadTemplateForCurrentEntreprise(UUID id) {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + id));
        String currentCode = entrepriseService.getCurrentCodeEntreprise();
        if (currentCode == null || !currentCode.equals(template.getCodeEntreprise())) {
            throw new TemplateNotFoundException("Template introuvable : " + id);
        }
        return template;
    }

    public TemplateSchemaDto getSchema(UUID templateId) {
        ReportTemplate template = loadTemplateForCurrentEntreprise(templateId);

        List<VariableResponse> variables = new ArrayList<>();
        String schemaJson = template.getSchema();   // <-- lecture du nouveau champ

        if (schemaJson != null && !schemaJson.isBlank()) {
            System.out.println("Schema JSON : " + schemaJson);   // juste avant le try
            try {
                JsonNode array = objectMapper.readTree(schemaJson);
                if (array.isArray()) {
                    for (JsonNode node : array) {
                        VariableResponse var = VariableResponse.builder()
                                .nomVariable(node.path("nomVariable").asText())
                                .type(VariableType.valueOf(node.path("type").asText()))
                                .obligatoire(node.path("obligatoire").asBoolean())
                                .build();
                        variables.add(var);
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException("Erreur lecture schéma JSON", e);
            }
        }

        return TemplateSchemaDto.builder()
                .templateId(template.getId())
                .nom(template.getNom())
                .version(template.getVersion())
                .variables(variables)
                .build();
    }
}