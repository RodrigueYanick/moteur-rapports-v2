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
    private final EntrepriseService entrepriseService;

    public VariableService(ReportVariableRepository variableRepository,
                           ReportTemplateRepository templateRepository,
                           VariableMapper variableMapper,
                           EntrepriseService entrepriseService) {
        this.variableRepository = variableRepository;
        this.templateRepository = templateRepository;
        this.variableMapper = variableMapper;
        this.entrepriseService = entrepriseService;
    }

    /**
     * Charge un template et vérifie son appartenance à l'entreprise courante.
     * Même pattern que ReportTemplateService.loadTemplateForCurrentEntreprise().
     */
    private ReportTemplate loadTemplateForCurrentEntreprise(UUID id) {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + id));
        String currentCode = entrepriseService.getCurrentCodeEntreprise();
        
        // Si le template est public (codeEntreprise null), accessible à tous
        if (template.getCodeEntreprise() == null || template.getCodeEntreprise().isBlank()) {
            return template;
        }
        // Si le template est privé, le header doit correspondre
        if (currentCode == null || !currentCode.equals(template.getCodeEntreprise())) {
            throw new TemplateNotFoundException("Template introuvable : " + id);
        }
        return template;
    }

    public List<VariableResponse> getVariables(@NonNull UUID templateId) {
        loadTemplateForCurrentEntreprise(templateId);

        return variableRepository.findByTemplate_Id(templateId)
                .stream()
                .map(variableMapper::toDto)
                .toList();
    }

    @Transactional
    public VariableResponse addVariable(@NonNull UUID templateId, VariableRequest request) {
        ReportTemplate template = loadTemplateForCurrentEntreprise(templateId);

        if (template.getStatut() != TemplateStatus.BROUILLON) {
            throw new ValidationException("Impossible d'ajouter des variables à un template publié ou archivé");
        }

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

    @Transactional
    public void deleteVariable(@NonNull UUID templateId, @NonNull UUID variableId) {
        ReportTemplate template = loadTemplateForCurrentEntreprise(templateId);

        ReportVariable variable = variableRepository.findById(variableId)
                .orElseThrow(() -> new ValidationException("Variable introuvable : " + variableId));
        if (!variable.getTemplate().getId().equals(templateId)) {
            throw new ValidationException("La variable n'appartient pas à ce template");
        }

        if (template.getStatut() != TemplateStatus.BROUILLON) {
            throw new ValidationException("Impossible de supprimer une variable d'un template publié ou archivé");
        }

        variableRepository.delete(variable);
    }

    @Transactional
    public VariableResponse updateVariable(UUID templateId, UUID variableId, VariableRequest request) {
        ReportTemplate template = loadTemplateForCurrentEntreprise(templateId);

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