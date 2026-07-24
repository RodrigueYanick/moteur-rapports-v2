package com.rapports.moteur.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.dtoTemplate.TemplateCreate;
import com.rapports.moteur.dto.dtoTemplate.TemplateResponse;
import com.rapports.moteur.dto.dtoVariable.ExtractedVariable;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.entity.TemplateStatus;
import com.rapports.moteur.exceptions.TemplateNotFoundException;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.mapper.TemplateMapper;
import com.rapports.moteur.repository.ReportTemplateRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportTemplateService {

    private final ReportTemplateRepository repository;
    private final TemplateMapper mapper;
    private final SchemaExtractorService schemaExtractorService;
    private final ObjectMapper objectMapper;

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
    public TemplateResponse publish(@NonNull UUID id) {
        ReportTemplate entity = repository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + id));

        if (entity.getStatut() != TemplateStatus.BROUILLON) {
            throw new ValidationException("Seul un template en brouillon peut être publié");
        }

        List<ExtractedVariable> variablesExtraites = schemaExtractorService.extraire(entity.getContenuDesign());
        try {
            entity.setSchema(objectMapper.writeValueAsString(variablesExtraites));
        } catch (JsonProcessingException e) {
            throw new ValidationException("Impossible de sérialiser le schéma extrait : " + e.getMessage());
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


}
