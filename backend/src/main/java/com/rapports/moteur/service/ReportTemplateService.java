package com.rapports.moteur.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rapports.moteur.dto.dtoTemplate.TemplateCreate;
import com.rapports.moteur.dto.dtoTemplate.TemplateResponse;
import com.rapports.moteur.dto.dtoVariable.ExtractedVariable;
import com.rapports.moteur.entity.Categorie;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.entity.ReportVariable;
import com.rapports.moteur.entity.TemplateStatus;
import com.rapports.moteur.exceptions.TemplateNotFoundException;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.mapper.TemplateMapper;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.repository.ReportVariableRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportTemplateService {

    private final ReportTemplateRepository repository;
    private final TemplateMapper mapper;
    private final SchemaExtractorService schemaExtractorService;
    private final ObjectMapper objectMapper;
    private final ReportVariableRepository variableRepository;
    private final EntrepriseService entrepriseService; 
    


    public List<TemplateResponse> findAll() {
        String code = entrepriseService.getCurrentCodeEntreprise();
        List<ReportTemplate> templates = repository.findByCodeEntreprise(code);
        return templates.stream().map(mapper::toDto).collect(Collectors.toList());
    }

    public TemplateResponse create(TemplateCreate request) {
        ReportTemplate entity = mapper.toEntity(request);
        entity.setStatut(TemplateStatus.BROUILLON);
        entity.setVersion(1);
        entity.setCodeEntreprise(entrepriseService.getCurrentCodeEntreprise()); 

        // Valeurs par défaut si non fournies
        if (entity.getCategorie() == null) entity.setCategorie(Categorie.AUTRES);
        if (entity.getFormatPapier() == null) entity.setFormatPapier("A4");

        // Design par défaut
        if (entity.getContenuDesign() == null || entity.getContenuDesign().isBlank()) {
            entity.setContenuDesign("{\"blocs\":[]}");
        }

        ReportTemplate saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    public TemplateResponse findById(@NonNull UUID id) {
        ReportTemplate template = repository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + id));
        return mapper.toDto(template);
    }

    public void delete(@NonNull UUID id) {
        repository.deleteById(id);
    }

    /**
     * Publie un template : extrait automatiquement le schéma des variables depuis
     * contenuDesign, change le statut en PUBLIE et incrémente la version.
     * Seul un template en BROUILLON peut être publié.
     *
     * À partir de la publication, la table ReportVariable n'est plus utilisée pour
     * ce template : le schéma extrait (champ "schema") devient la seule source de
     * vérité des variables à compléter (voir SchemaService.getSchema).
     */
    @Transactional
    public TemplateResponse publish(UUID id) {
        ReportTemplate entity = repository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + id));
        if (entity.getStatut() != TemplateStatus.BROUILLON) {
            throw new ValidationException("Seul un template en brouillon peut être publié");
        }

        // Priorité aux variables définies explicitement par l'utilisateur
        List<ReportVariable> explicitVariables = variableRepository.findByTemplate_Id(id);
        List<ExtractedVariable> variablesToStore;

        if (!explicitVariables.isEmpty()) {
            // Utilise les variables explicites
            variablesToStore = explicitVariables.stream()
                    .map(this::mapToExtractedVariable)
                    .collect(Collectors.toList());
        } else {
            // Fallback : extraction automatique depuis le design
            if (entity.getContenuDesign() != null && !entity.getContenuDesign().isBlank()) {
                variablesToStore = schemaExtractorService.extract(entity.getContenuDesign());
            } else {
                variablesToStore = Collections.emptyList();
            }
        }

        // Construit le JSON du schéma
        String variablesJson = buildVariablesJson(variablesToStore);
        entity.setSchema(variablesJson);

        // Passe en statut publié
        entity.setStatut(TemplateStatus.PUBLIE);
        entity.setVersion(entity.getVersion() + 1);
        repository.save(entity);
        return mapper.toDto(entity);
    }

    // Convertit une ReportVariable (entité) en ExtractedVariable (utilisé pour le JSON)
    private ExtractedVariable mapToExtractedVariable(ReportVariable variable) {
        return ExtractedVariable.builder()
                .nom(variable.getNomVariable())
                .type(variable.getType().name())
                .obligatoire(variable.getObligatoire())
                .build();
    }

// Construit le JSON à partir de la liste d'ExtractedVariable (existant, inchangé)
    private String buildVariablesJson(List<ExtractedVariable> variables) {
        ArrayNode array = objectMapper.createArrayNode();
        for (ExtractedVariable var : variables) {
            ObjectNode node = array.addObject();
            node.put("nomVariable", var.getNom());
            node.put("type", var.getType());
            node.put("obligatoire", var.getObligatoire());
        }
        try {
            return objectMapper.writeValueAsString(array);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Erreur génération JSON variables", e);
        }
    }

    /**
     * Met à jour les champs modifiables d'un template (uniquement s'il est en mode BROUILLON).
     * Les champs autorisés : nom, description, contenuDesign.
     */
    @Transactional
    public TemplateResponse update(@NonNull UUID id, TemplateCreate request) {
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

    @Transactional
    public TemplateResponse duplicate(UUID id) {
        ReportTemplate original = repository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + id));

        ReportTemplate copy = new ReportTemplate();
        copy.setNom(original.getNom() + " (copie)");
        copy.setDescription(original.getDescription());
        copy.setContenuDesign(original.getContenuDesign());
        copy.setCategorie(original.getCategorie());
        copy.setFormatPapier(original.getFormatPapier());
        copy.setStatut(TemplateStatus.BROUILLON);
        copy.setVersion(1);

        ReportTemplate savedCopy = repository.save(copy);

        // 🔁 Duplication des variables explicites
        List<ReportVariable> originalVariables = variableRepository.findByTemplate_Id(id);
        List<ReportVariable> newVariables = new ArrayList<>();
        for (ReportVariable var : originalVariables) {
            ReportVariable newVar = ReportVariable.builder()
                    .template(savedCopy)
                    .nomVariable(var.getNomVariable())
                    .type(var.getType())
                    .obligatoire(var.getObligatoire())
                    .description(var.getDescription())
                    .build();
            newVariables.add(newVar);
        }
        variableRepository.saveAll(newVariables);

        return mapper.toDto(savedCopy);
    }

    @Transactional
    public TemplateResponse archive(UUID id) {
        ReportTemplate entity = repository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + id));
        if (entity.getStatut() == TemplateStatus.ARCHIVE) {
            throw new ValidationException("Le template est déjà archivé");
        }
        entity.setStatut(TemplateStatus.ARCHIVE);
        repository.save(entity);
        return mapper.toDto(entity);
    }

    @Transactional
    public TemplateResponse newVersion(UUID id) {
        ReportTemplate original = repository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + id));

        // Si l'original n'est pas déjà archivé, on l'archive
        if (original.getStatut() != TemplateStatus.ARCHIVE) {
            original.setStatut(TemplateStatus.ARCHIVE);
            repository.save(original);
        }

        // Créer le brouillon de la nouvelle version
        ReportTemplate newVersion = new ReportTemplate();
        newVersion.setNom(original.getNom() + " (V" + (original.getVersion() + 1) + ")");
        newVersion.setDescription(original.getDescription());
        newVersion.setContenuDesign(original.getContenuDesign());
        newVersion.setCategorie(original.getCategorie());
        newVersion.setFormatPapier(original.getFormatPapier());
        newVersion.setCodeEntreprise(original.getCodeEntreprise());
        newVersion.setStatut(TemplateStatus.BROUILLON);
        newVersion.setVersion(original.getVersion() + 1);
        newVersion.setParentTemplate(original);

        ReportTemplate savedNew = repository.save(newVersion);

        // Copier les variables explicites de l'original vers la nouvelle version
        List<ReportVariable> originalVariables = variableRepository.findByTemplate_Id(id);
        List<ReportVariable> newVariables = new ArrayList<>();
        for (ReportVariable var : originalVariables) {
            ReportVariable newVar = ReportVariable.builder()
                    .template(savedNew)
                    .nomVariable(var.getNomVariable())
                    .type(var.getType())
                    .obligatoire(var.getObligatoire())
                    .description(var.getDescription())
                    .build();
            newVariables.add(newVar);
        }
        variableRepository.saveAll(newVariables);

        return mapper.toDto(savedNew);
    }

    @Transactional
    public TemplateResponse restore(UUID id) {
        ReportTemplate entity = repository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + id));
        if (entity.getStatut() != TemplateStatus.ARCHIVE) {
            throw new ValidationException("Seul un template archivé peut être restauré");
        }
        entity.setStatut(TemplateStatus.BROUILLON);
        repository.save(entity);
        return mapper.toDto(entity);
    }

}
