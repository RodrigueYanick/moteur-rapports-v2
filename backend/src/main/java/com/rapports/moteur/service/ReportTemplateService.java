package com.rapports.moteur.service;

import com.rapports.moteur.dto.dtoTemplate.TemplateCreate;
import com.rapports.moteur.dto.dtoTemplate.TemplateResponse;
import com.rapports.moteur.dto.dtoVariable.VariableRequest;
import com.rapports.moteur.dto.dtoVariable.VariableResponse;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.entity.ReportVariable;
import com.rapports.moteur.entity.TemplateStatus;
import com.rapports.moteur.exceptions.TemplateNotFoundException;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.mapper.TemplateMapper;
import com.rapports.moteur.mapper.VariableMapper;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.repository.ReportVariableRepository;

import jakarta.transaction.Transactional;
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
        ReportTemplate entity = mapper.toEntity(request);
        entity.setStatut(TemplateStatus.BROUILLON);
        entity.setVersion(1);
        // Valeur par défaut pour éviter les null
        if (entity.getContenuDesign() == null || entity.getContenuDesign().isBlank()) {
            entity.setContenuDesign("""
                { "blocs": [
                    { "type": "titre", "contenu": "Rapport {{nom_template}}" },
                    { "type": "texte", "contenu": "Données fournies :" }
                ]}
            """);
        }
        ReportTemplate saved = repository.save(entity);
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

    /**
     * Publie un template : change le statut en PUBLIE et incrémente la version.
     * Seul un template en BROUILLON peut être publié.
     */
    @Transactional
    public TemplateResponse publish(UUID id) {
        ReportTemplate entity = repository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + id));

        if (entity.getStatut() != TemplateStatus.BROUILLON) {
            throw new ValidationException("Seul un template en brouillon peut être publié");
        }

        entity.setStatut(TemplateStatus.PUBLIE);
        entity.setVersion(entity.getVersion() + 1);
        // la date de modification est mise à jour par @PreUpdate
        repository.save(entity);
        return mapper.toDto(entity);
    }


    /**
     * Met à jour les champs modifiables d'un template (uniquement s'il est en mode BROUILLON).
     * Les champs autorisés : nom, description, contenuDesign.
     */
    @Transactional
    public TemplateResponse update(UUID id, TemplateCreate request) {
        ReportTemplate entity = repository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + id));

        if (entity.getStatut() != TemplateStatus.BROUILLON) {
            throw new ValidationException("Seul un template en brouillon peut être modifié");
        }

        // Mise à jour des champs autorisés
        entity.setNom(request.getNom());
        entity.setDescription(request.getDescription());
        entity.setContenuDesign(request.getContenuDesign());

        // La date de modification sera automatiquement mise à jour par @PreUpdate
        repository.save(entity);
        return mapper.toDto(entity);
    }


}
