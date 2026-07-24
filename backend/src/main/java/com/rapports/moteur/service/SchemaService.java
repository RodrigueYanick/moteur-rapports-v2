package com.rapports.moteur.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.dtoTemplate.TemplateSchemaDto;
import com.rapports.moteur.dto.dtoVariable.ExtractedVariable;
import com.rapports.moteur.dto.dtoVariable.VariableResponse;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.exceptions.TemplateNotFoundException;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.repository.ReportTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

@Service
public class SchemaService {

    private final ReportTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    public SchemaService(ReportTemplateRepository templateRepository,
                          ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Retourne le schema des variables d'un template, tel qu'extrait
     * automatiquement lors de sa publication (champ ReportTemplate.schema).
     *
     * ReportVariable n'est plus utilise ici : un template non encore publie
     * n'a pas de schema extrait, et la liste de variables renvoyee est donc vide.
     */
    public TemplateSchemaDto getSchema(@NonNull UUID templateId) {
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + templateId));

        List<ExtractedVariable> variablesExtraites = lireSchema(template.getSchema());

        List<VariableResponse> variables = variablesExtraites.stream()
                .map(v -> VariableResponse.builder()
                        // Pas persistee individuellement : identifiant transitoire genere
                        // uniquement pour respecter le contrat existant avec le frontend.
                        .id(UUID.randomUUID())
                        .nomVariable(v.getNom())
                        .type(v.getType())
                        .obligatoire(v.getObligatoire())
                        .build())
                .toList();

        return TemplateSchemaDto.builder()
                .templateId(template.getId())
                .nom(template.getNom())
                .version(template.getVersion())
                .variables(variables)
                .build();
    }

    private List<ExtractedVariable> lireSchema(String schemaJson) {
        if (schemaJson == null || schemaJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(schemaJson, new TypeReference<List<ExtractedVariable>>() {});
        } catch (JsonProcessingException e) {
            throw new ValidationException("Schema stocke invalide pour ce template : " + e.getMessage());
        }
    }
}
