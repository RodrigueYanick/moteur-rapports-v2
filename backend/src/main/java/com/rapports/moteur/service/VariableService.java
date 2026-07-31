package com.rapports.moteur.service;

import com.rapports.moteur.dto.dtoVariable.VariableRequest;
import com.rapports.moteur.dto.dtoVariable.VariableResponse;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.entity.ReportVariable;
import com.rapports.moteur.entity.TemplateStatus;
import com.rapports.moteur.exceptions.TemplateNotFoundException;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.mapper.VariableMapper;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.repository.ReportVariableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import org.springframework.lang.NonNull;

@Service
public class VariableService {

    private final ReportVariableRepository variableRepository;
    private final ReportTemplateRepository templateRepository;
    private final VariableMapper variableMapper;

    public VariableService(ReportVariableRepository variableRepository,
                           ReportTemplateRepository templateRepository,
                           VariableMapper variableMapper) {
        this.variableRepository = variableRepository;
        this.templateRepository = templateRepository;
        this.variableMapper = variableMapper;
    }

    /**
     * Récupère toutes les variables d'un template donné.
     */
    public List<VariableResponse> getVariables(@NonNull UUID templateId) {
        // Vérifie que le template existe (optionnel, mais recommandé)
        templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + templateId));

        return variableRepository.findByTemplate_Id(templateId)
                .stream()
                .map(variableMapper::toDto)
                .toList();
    }

    /**
     * Ajoute une variable à un template (uniquement si le template est en brouillon).
     */
    @Transactional
    public VariableResponse addVariable(@NonNull UUID templateId, VariableRequest request) {
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + templateId));

        // Seul un template en mode BROUILLON peut recevoir de nouvelles variables
        if (template.getStatut() != TemplateStatus.BROUILLON) {
            throw new ValidationException("Impossible d'ajouter des variables à un template publié ou archivé");
        }

        // Vérifie si la variable existe déjà (évite les doublons)
        boolean existe = variableRepository.findByTemplate_Id(templateId)
                .stream()
                .anyMatch(v -> v.getNomVariable().equalsIgnoreCase(request.getNomVariable()));
        if (existe) {
            throw new ValidationException("Une variable avec le nom '" + request.getNomVariable() + "' existe déjà");
        }

        ReportVariable entity = variableMapper.toEntity(request);
        entity.setTemplate(template);
        ReportVariable saved = variableRepository.save(entity);
        return variableMapper.toDto(saved);
    }

    /**
     * Supprime une variable (uniquement si le template est en brouillon).
     */
    @Transactional
    public void deleteVariable(@NonNull UUID templateId, @NonNull UUID variableId) {
        // Vérifie que le template existe
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + templateId));

        // Vérifie que la variable existe et appartient bien au template
        ReportVariable variable = variableRepository.findById(variableId)
                .orElseThrow(() -> new ValidationException("Variable introuvable : " + variableId));
        if (!variable.getTemplate().getId().equals(templateId)) {
            throw new ValidationException("La variable n'appartient pas à ce template");
        }

        // Suppression autorisée uniquement si le template est en brouillon
        if (template.getStatut() != TemplateStatus.BROUILLON) {
            throw new ValidationException("Impossible de supprimer une variable d'un template publié ou archivé");
        }

        variableRepository.delete(variable);
    }

    @Transactional
    public VariableResponse updateVariable(UUID templateId, UUID variableId, VariableRequest request) {
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable"));
        if (template.getStatut() != TemplateStatus.BROUILLON) {
            throw new ValidationException("Modification impossible sur un template publié");
        }
        ReportVariable variable = variableRepository.findById(variableId)
                .orElseThrow(() -> new ValidationException("Variable introuvable"));
        if (!variable.getTemplate().getId().equals(templateId)) {
            throw new ValidationException("Variable n'appartenant pas à ce template");
        }
        variable.setNomVariable(request.getNomVariable());
        variable.setType(request.getType());
        variable.setObligatoire(request.getObligatoire());
        variable.setDescription(request.getDescription());
        return variableMapper.toDto(variableRepository.save(variable));
    }
}