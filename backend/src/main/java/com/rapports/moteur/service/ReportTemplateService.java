package com.rapports.moteur.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rapports.moteur.dto.dtoTemplate.TemplateCreate;
import com.rapports.moteur.dto.dtoTemplate.TemplateResponse;
import com.rapports.moteur.dto.dtoTemplate.TemplateVersionDto;
import com.rapports.moteur.dto.dtoTemplate.TemplateVersionTreeDto;
import com.rapports.moteur.dto.dtoVariable.ExtractedVariable;
import com.rapports.moteur.entity.Categorie;
import com.rapports.moteur.entity.PaginationMode;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.entity.ReportVariable;
import com.rapports.moteur.entity.TemplateStatus;
import com.rapports.moteur.entity.Visibilite;
import java.util.Comparator;
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
    private final CompanyWorkspaceConfigService workspaceConfigService;
    

    // ============================================================
    // Contrôle d'appartenance multi-entreprise — point d'entrée unique
    // ============================================================
    /**
     * Charge un template et vérifie qu'il appartient bien à l'entreprise courante
     * (déduite du header X-Entreprise-Code via EntrepriseService).
     * Si le template appartient à une autre entreprise, on renvoie la même
     * exception que "template introuvable" : on ne révèle jamais qu'un template
     * d'une autre entreprise existe avec cet id.
     */
    private ReportTemplate loadTemplateForCurrentEntreprise(UUID id) {
        ReportTemplate template = repository.findById(id)
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

    public List<TemplateResponse> findAll(Visibilite visibilite, String q) {
        String code = entrepriseService.getCurrentCodeEntreprise();
        List<ReportTemplate> templates;

        String search = (q != null && !q.isBlank()) ? q.trim() : null;

        if (visibilite == null) {
            // Ancien comportement : selon la présence du header
            if (code != null) {
                templates = (search != null)
                    ? repository.findByCodeEntrepriseOrCodeEntrepriseIsNullAndNomContaining(code, search)
                    : repository.findByCodeEntrepriseOrCodeEntrepriseIsNull(code);
            } else {
                templates = (search != null)
                    ? repository.findByCodeEntrepriseIsNullAndNomContaining(search)
                    : repository.findByCodeEntrepriseIsNull();
            }
        } else {
            switch (visibilite) {
                case PRIVATE:
                    if (code == null) {
                        // Un utilisateur sans header ne peut pas avoir de templates privés
                        templates = List.of();
                    } else {
                        templates = (search != null)
                            ? repository.findByCodeEntrepriseAndNomContaining(code, search)
                            : repository.findByCodeEntreprise(code);
                    }
                    break;
                case PUBLIC:
                    templates = (search != null)
                        ? repository.findByCodeEntrepriseIsNullAndNomContaining(search)
                        : repository.findByCodeEntrepriseIsNull();
                    break;
                case ALL:
                default:
                    if (code != null) {
                        templates = (search != null)
                            ? repository.findByCodeEntrepriseOrCodeEntrepriseIsNullAndNomContaining(code, search)
                            : repository.findByCodeEntrepriseOrCodeEntrepriseIsNull(code);
                    } else {
                        templates = (search != null)
                            ? repository.findByCodeEntrepriseIsNullAndNomContaining(search)
                            : repository.findByCodeEntrepriseIsNull();
                    }
                    break;
            }
        }

        return templates.stream().map(mapper::toDto).collect(Collectors.toList());
    }

    private void validateFormat(ReportTemplate template) {
        if ("CUSTOM".equalsIgnoreCase(template.getFormatPapier())) {
            if (template.getLargeurMm() == null || template.getHauteurMm() == null
                || template.getLargeurMm() <= 0 || template.getHauteurMm() <= 0) {
                throw new ValidationException("Les dimensions personnalisées (largeurMm, hauteurMm) sont obligatoires pour un format CUSTOM");
            }
        } else {
            // Pour les formats standards, on ignore/annule les dimensions personnalisées
            template.setLargeurMm(null);
            template.setHauteurMm(null);
        }
    }

    public TemplateResponse create(TemplateCreate request) {
        ReportTemplate entity = mapper.toEntity(request);
        if (entity.getModePagination() == null) entity.setModePagination(PaginationMode.FIXED);
        entity.setStatut(TemplateStatus.BROUILLON);
        entity.setVersion(1);
        entity.setCodeEntreprise(entrepriseService.getCurrentCodeEntreprise());

        if (entity.getCategorie() == null) entity.setCategorie(Categorie.AUTRES);
        if (entity.getFormatPapier() == null) entity.setFormatPapier("A4");
        if (entity.getMargeGaucheMm() == null) entity.setMargeGaucheMm(10);
        if (entity.getMargeDroiteMm() == null) entity.setMargeDroiteMm(10);
        if (entity.getMargeHautMm() == null) entity.setMargeHautMm(10);
        if (entity.getMargeBasMm() == null) entity.setMargeBasMm(10);

        // Héritage automatique des paramètres de feuille de travail de l'entreprise
        com.rapports.moteur.entity.CompanyWorkspaceConfig defaultCfg = workspaceConfigService.getEntityForCurrentEntreprise();
        if (request.getFormatPapier() == null) entity.setFormatPapier(defaultCfg.getFormatPapier());
        if (request.getLargeurMm() == null) entity.setLargeurMm(defaultCfg.getLargeurMm());
        if (request.getHauteurMm() == null) entity.setHauteurMm(defaultCfg.getHauteurMm());
        if (request.getModePagination() == null) entity.setModePagination(defaultCfg.getModePagination());
        if (request.getMargeGaucheMm() == null) entity.setMargeGaucheMm(defaultCfg.getMargeGaucheMm());
        if (request.getMargeDroiteMm() == null) entity.setMargeDroiteMm(defaultCfg.getMargeDroiteMm());
        if (request.getMargeHautMm() == null) entity.setMargeHautMm(defaultCfg.getMargeHautMm());
        if (request.getMargeBasMm() == null) entity.setMargeBasMm(defaultCfg.getMargeBasMm());
        if (request.getCouleurFond() == null) entity.setCouleurFond(defaultCfg.getCouleurFond());
        if (request.getHeaderActif() == null) entity.setHeaderActif(defaultCfg.getHeaderActif());
        if (request.getHauteurHeaderMm() == null) entity.setHauteurHeaderMm(defaultCfg.getHauteurHeaderMm());
        if (request.getHeaderContenu() == null) entity.setHeaderContenu(defaultCfg.getHeaderContenu());
        if (request.getHeaderAlignement() == null) entity.setHeaderAlignement(defaultCfg.getHeaderAlignement());
        if (request.getHeaderAfficherSurPremierePage() == null) entity.setHeaderAfficherSurPremierePage(defaultCfg.getHeaderAfficherSurPremierePage());
        if (request.getHeaderLigneSeparation() == null) entity.setHeaderLigneSeparation(defaultCfg.getHeaderLigneSeparation());
        if (request.getHeaderCouleurLigne() == null) entity.setHeaderCouleurLigne(defaultCfg.getHeaderCouleurLigne());
        if (request.getFooterActif() == null) entity.setFooterActif(defaultCfg.getFooterActif());
        if (request.getHauteurFooterMm() == null) entity.setHauteurFooterMm(defaultCfg.getHauteurFooterMm());
        if (request.getFooterContenu() == null) entity.setFooterContenu(defaultCfg.getFooterContenu());
        if (request.getFooterAlignement() == null) entity.setFooterAlignement(defaultCfg.getFooterAlignement());
        if (request.getFooterAfficherSurPremierePage() == null) entity.setFooterAfficherSurPremierePage(defaultCfg.getFooterAfficherSurPremierePage());
        if (request.getFooterLigneSeparation() == null) entity.setFooterLigneSeparation(defaultCfg.getFooterLigneSeparation());
        if (request.getFooterCouleurLigne() == null) entity.setFooterCouleurLigne(defaultCfg.getFooterCouleurLigne());
        if (request.getNumerotationPage() == null) entity.setNumerotationPage(defaultCfg.getNumerotationPage());
        if (request.getFormatNumerotation() == null) entity.setFormatNumerotation(defaultCfg.getFormatNumerotation());

        validateFormat(entity);

        if (entity.getContenuDesign() == null || entity.getContenuDesign().isBlank()) {
            entity.setContenuDesign("{\"blocs\":[]}");
        }

        ReportTemplate saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    public TemplateResponse findById(@NonNull UUID id) {
        ReportTemplate template = loadTemplateForCurrentEntreprise(id);
        return mapper.toDto(template);
    }

    @Transactional
    public void delete(@NonNull UUID id) {
        // Avant : repository.deleteById(id) sans aucune vérification d'existence
        // ni d'appartenance -> n'importe qui pouvait supprimer n'importe quel
        // template d'une autre entreprise en connaissant juste son UUID.
        ReportTemplate template = loadTemplateForCurrentEntreprise(id);
        repository.delete(template);
    }

    /**
     * Publie un template : extrait automatiquement le schéma des variables depuis
     * contenuDesign, change le statut en PUBLIE et incrémente la version.
     * contenuDesign et change le statut en PUBLIE.
     * Seul un template en BROUILLON peut être publié.
     */
    @Transactional
    public TemplateResponse publish(UUID id) {
        ReportTemplate entity = loadTemplateForCurrentEntreprise(id);
        if (entity.getStatut() != TemplateStatus.BROUILLON) {
            throw new ValidationException("Seul un template en brouillon peut être publié");
        }

        List<ReportVariable> explicitVariables = variableRepository.findByTemplate_Id(id);
        List<ExtractedVariable> variablesToStore;

        if (!explicitVariables.isEmpty()) {
            variablesToStore = explicitVariables.stream()
                    .map(this::mapToExtractedVariable)
                    .collect(Collectors.toList());
        } else {
            if (entity.getContenuDesign() != null && !entity.getContenuDesign().isBlank()) {
                variablesToStore = schemaExtractorService.extract(entity.getContenuDesign());
            } else {
                variablesToStore = Collections.emptyList();
            }
        }

        String variablesJson = buildVariablesJson(variablesToStore);
        entity.setSchema(variablesJson);

        entity.setStatut(TemplateStatus.PUBLIE);
        entity.setVersion(entity.getVersion() + 1);
        repository.save(entity);
        return mapper.toDto(entity);
    }

    private ExtractedVariable mapToExtractedVariable(ReportVariable variable) {
        return ExtractedVariable.builder()
                .nom(variable.getNomVariable())
                .type(variable.getType().name())
                .obligatoire(variable.getObligatoire())
                .build();
    }

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
     */
    @Transactional
    public TemplateResponse update(@NonNull UUID id, TemplateCreate request) {
        ReportTemplate entity = loadTemplateForCurrentEntreprise(id);

        if (entity.getStatut() != TemplateStatus.BROUILLON) {
            throw new ValidationException("Seul un template en brouillon peut être modifié");
        }

        if (request.getNom() != null) entity.setNom(request.getNom());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getContenuDesign() != null) entity.setContenuDesign(request.getContenuDesign());
        if (request.getFormatPapier() != null) entity.setFormatPapier(request.getFormatPapier());
        if (request.getLargeurMm() != null) entity.setLargeurMm(request.getLargeurMm());
        if (request.getHauteurMm() != null) entity.setHauteurMm(request.getHauteurMm());
        if (request.getModePagination() != null) entity.setModePagination(request.getModePagination());
        if (request.getMargeGaucheMm() != null) entity.setMargeGaucheMm(request.getMargeGaucheMm());
        if (request.getMargeDroiteMm() != null) entity.setMargeDroiteMm(request.getMargeDroiteMm());
        if (request.getMargeHautMm() != null) entity.setMargeHautMm(request.getMargeHautMm());
        if (request.getMargeBasMm() != null) entity.setMargeBasMm(request.getMargeBasMm());
        if (request.getCouleurFond() != null) entity.setCouleurFond(request.getCouleurFond());

        // Header
        if (request.getHeaderActif() != null) entity.setHeaderActif(request.getHeaderActif());
        if (request.getHauteurHeaderMm() != null) entity.setHauteurHeaderMm(request.getHauteurHeaderMm());
        if (request.getHeaderContenu() != null) entity.setHeaderContenu(request.getHeaderContenu());
        if (request.getHeaderAlignement() != null) entity.setHeaderAlignement(request.getHeaderAlignement());
        if (request.getHeaderAfficherSurPremierePage() != null) entity.setHeaderAfficherSurPremierePage(request.getHeaderAfficherSurPremierePage());
        if (request.getHeaderLigneSeparation() != null) entity.setHeaderLigneSeparation(request.getHeaderLigneSeparation());
        if (request.getHeaderCouleurLigne() != null) entity.setHeaderCouleurLigne(request.getHeaderCouleurLigne());

        // Footer
        if (request.getFooterActif() != null) entity.setFooterActif(request.getFooterActif());
        if (request.getHauteurFooterMm() != null) entity.setHauteurFooterMm(request.getHauteurFooterMm());
        if (request.getFooterContenu() != null) entity.setFooterContenu(request.getFooterContenu());
        if (request.getFooterAlignement() != null) entity.setFooterAlignement(request.getFooterAlignement());
        if (request.getFooterAfficherSurPremierePage() != null) entity.setFooterAfficherSurPremierePage(request.getFooterAfficherSurPremierePage());
        if (request.getFooterLigneSeparation() != null) entity.setFooterLigneSeparation(request.getFooterLigneSeparation());
        if (request.getFooterCouleurLigne() != null) entity.setFooterCouleurLigne(request.getFooterCouleurLigne());
        if (request.getNumerotationPage() != null) entity.setNumerotationPage(request.getNumerotationPage());
        if (request.getFormatNumerotation() != null) entity.setFormatNumerotation(request.getFormatNumerotation());

        validateFormat(entity);

        repository.save(entity);
        return mapper.toDto(entity);
    }

    @Transactional
    public TemplateResponse duplicate(UUID id) {
        ReportTemplate original = loadTemplateForCurrentEntreprise(id);

        ReportTemplate copy = new ReportTemplate();
        copy.setNom(original.getNom() + " (copie)");
        copy.setDescription(original.getDescription());
        copy.setContenuDesign(original.getContenuDesign());
        copy.setCategorie(original.getCategorie());
        copy.setFormatPapier(original.getFormatPapier());
        copy.setLargeurMm(original.getLargeurMm());
        copy.setHauteurMm(original.getHauteurMm());
        copy.setCodeEntreprise(original.getCodeEntreprise());
        copy.setModePagination(original.getModePagination() != null ? original.getModePagination() : PaginationMode.FIXED);
        copy.setMargeGaucheMm(original.getMargeGaucheMm() != null && original.getMargeGaucheMm() > 0 ? original.getMargeGaucheMm() : 10);
        copy.setMargeDroiteMm(original.getMargeDroiteMm() != null && original.getMargeDroiteMm() > 0 ? original.getMargeDroiteMm() : 10);
        copy.setMargeHautMm(original.getMargeHautMm() != null && original.getMargeHautMm() > 0 ? original.getMargeHautMm() : 10);
        copy.setMargeBasMm(original.getMargeBasMm() != null && original.getMargeBasMm() > 0 ? original.getMargeBasMm() : 10);
        copy.setCouleurFond(original.getCouleurFond() != null ? original.getCouleurFond() : "#ffffff");

        // Header
        copy.setHeaderActif(original.getHeaderActif() != null ? original.getHeaderActif() : false);
        copy.setHauteurHeaderMm(original.getHauteurHeaderMm() != null ? original.getHauteurHeaderMm() : 15);
        copy.setHeaderContenu(original.getHeaderContenu());
        copy.setHeaderAlignement(original.getHeaderAlignement() != null ? original.getHeaderAlignement() : "LEFT");
        copy.setHeaderAfficherSurPremierePage(original.getHeaderAfficherSurPremierePage() != null ? original.getHeaderAfficherSurPremierePage() : true);
        copy.setHeaderLigneSeparation(original.getHeaderLigneSeparation() != null ? original.getHeaderLigneSeparation() : false);
        copy.setHeaderCouleurLigne(original.getHeaderCouleurLigne() != null ? original.getHeaderCouleurLigne() : "#d1d5db");

        // Footer
        copy.setFooterActif(original.getFooterActif() != null ? original.getFooterActif() : false);
        copy.setHauteurFooterMm(original.getHauteurFooterMm() != null ? original.getHauteurFooterMm() : 15);
        copy.setFooterContenu(original.getFooterContenu());
        copy.setFooterAlignement(original.getFooterAlignement() != null ? original.getFooterAlignement() : "LEFT");
        copy.setFooterAfficherSurPremierePage(original.getFooterAfficherSurPremierePage() != null ? original.getFooterAfficherSurPremierePage() : true);
        copy.setFooterLigneSeparation(original.getFooterLigneSeparation() != null ? original.getFooterLigneSeparation() : false);
        copy.setFooterCouleurLigne(original.getFooterCouleurLigne() != null ? original.getFooterCouleurLigne() : "#d1d5db");
        copy.setNumerotationPage(original.getNumerotationPage() != null ? original.getNumerotationPage() : true);
        copy.setFormatNumerotation(original.getFormatNumerotation() != null ? original.getFormatNumerotation() : "PAGE_X_SUR_Y");

        copy.setStatut(TemplateStatus.BROUILLON);
        copy.setVersion(1);

        ReportTemplate savedCopy = repository.save(copy);

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
        ReportTemplate entity = loadTemplateForCurrentEntreprise(id);
        if (entity.getStatut() == TemplateStatus.ARCHIVE) {
            throw new ValidationException("Le template est déjà archivé");
        }
        entity.setStatut(TemplateStatus.ARCHIVE);
        repository.save(entity);
        return mapper.toDto(entity);
    }

    @Transactional
    public TemplateResponse newVersion(UUID id) {
        ReportTemplate original = loadTemplateForCurrentEntreprise(id);

        if (original.getStatut() != TemplateStatus.ARCHIVE) {
            original.setStatut(TemplateStatus.ARCHIVE);
            repository.save(original);
        }

        String baseName = original.getNom().replaceAll("\\s*\\(V\\d+\\)$", "").trim();
        int nextVersion = original.getVersion() + 1;

        ReportTemplate newVersion = new ReportTemplate();
        newVersion.setNom(original.getNom() + " (V" + (original.getVersion() + 1) + ")");
        newVersion.setNom(baseName + " (V" + nextVersion + ")");
        newVersion.setDescription(original.getDescription());
        newVersion.setContenuDesign(original.getContenuDesign());
        newVersion.setCategorie(original.getCategorie());
        newVersion.setFormatPapier(original.getFormatPapier());
        newVersion.setLargeurMm(original.getLargeurMm());
        newVersion.setHauteurMm(original.getHauteurMm());
        newVersion.setCodeEntreprise(original.getCodeEntreprise());
        newVersion.setModePagination(original.getModePagination() != null ? original.getModePagination() : PaginationMode.FIXED);
        newVersion.setMargeGaucheMm(original.getMargeGaucheMm() != null && original.getMargeGaucheMm() > 0 ? original.getMargeGaucheMm() : 10);
        newVersion.setMargeDroiteMm(original.getMargeDroiteMm() != null && original.getMargeDroiteMm() > 0 ? original.getMargeDroiteMm() : 10);
        newVersion.setMargeHautMm(original.getMargeHautMm() != null && original.getMargeHautMm() > 0 ? original.getMargeHautMm() : 10);
        newVersion.setMargeBasMm(original.getMargeBasMm() != null && original.getMargeBasMm() > 0 ? original.getMargeBasMm() : 10);
        newVersion.setCouleurFond(original.getCouleurFond() != null ? original.getCouleurFond() : "#ffffff");

        // Header
        newVersion.setHeaderActif(original.getHeaderActif() != null ? original.getHeaderActif() : false);
        newVersion.setHauteurHeaderMm(original.getHauteurHeaderMm() != null ? original.getHauteurHeaderMm() : 15);
        newVersion.setHeaderContenu(original.getHeaderContenu());
        newVersion.setHeaderAlignement(original.getHeaderAlignement() != null ? original.getHeaderAlignement() : "LEFT");
        newVersion.setHeaderAfficherSurPremierePage(original.getHeaderAfficherSurPremierePage() != null ? original.getHeaderAfficherSurPremierePage() : true);
        newVersion.setHeaderLigneSeparation(original.getHeaderLigneSeparation() != null ? original.getHeaderLigneSeparation() : false);
        newVersion.setHeaderCouleurLigne(original.getHeaderCouleurLigne() != null ? original.getHeaderCouleurLigne() : "#d1d5db");

        // Footer
        newVersion.setFooterActif(original.getFooterActif() != null ? original.getFooterActif() : false);
        newVersion.setHauteurFooterMm(original.getHauteurFooterMm() != null ? original.getHauteurFooterMm() : 15);
        newVersion.setFooterContenu(original.getFooterContenu());
        newVersion.setFooterAlignement(original.getFooterAlignement() != null ? original.getFooterAlignement() : "LEFT");
        newVersion.setFooterAfficherSurPremierePage(original.getFooterAfficherSurPremierePage() != null ? original.getFooterAfficherSurPremierePage() : true);
        newVersion.setFooterLigneSeparation(original.getFooterLigneSeparation() != null ? original.getFooterLigneSeparation() : false);
        newVersion.setFooterCouleurLigne(original.getFooterCouleurLigne() != null ? original.getFooterCouleurLigne() : "#d1d5db");
        newVersion.setNumerotationPage(original.getNumerotationPage() != null ? original.getNumerotationPage() : true);
        newVersion.setFormatNumerotation(original.getFormatNumerotation() != null ? original.getFormatNumerotation() : "PAGE_X_SUR_Y");

        newVersion.setStatut(TemplateStatus.BROUILLON);
        newVersion.setVersion(original.getVersion() + 1);
        newVersion.setVersion(nextVersion);
        newVersion.setParentTemplate(original);

        ReportTemplate savedNew = repository.save(newVersion);

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
        ReportTemplate entity = loadTemplateForCurrentEntreprise(id);
        if (entity.getStatut() != TemplateStatus.ARCHIVE) {
            throw new ValidationException("Seul un template archivé peut être restauré");
        }
        entity.setStatut(TemplateStatus.BROUILLON);
        repository.save(entity);
        return mapper.toDto(entity);
    }

    /**
     * Récupère l'arbre généalogique et l'historique complet des versions du modèle.
     * Remonte d'abord la chaîne parentale jusqu'à la racine de la famille,
     * puis explore récursivement tous les enfants pour produire à la fois l'arborescence
     * hiérarchique et la chronologie ordonnée de toutes les versions.
     */
    @Transactional
    public TemplateVersionTreeDto getVersionTree(UUID id) {
        ReportTemplate current = loadTemplateForCurrentEntreprise(id);

        // 1. Remonter la chaîne parentale jusqu'à la racine
        ReportTemplate root = current;
        while (root.getParentTemplate() != null) {
            root = root.getParentTemplate();
        }

        // 2. Construire l'arbre récursif et collecter les versions
        List<TemplateVersionDto> flatList = new ArrayList<>();
        TemplateVersionDto tree = buildVersionNode(root, current.getId(), flatList);

        // Trier la chronologie par version puis date
        flatList.sort(Comparator.comparing(TemplateVersionDto::getVersion)
                .thenComparing(TemplateVersionDto::getDateCreation));

        return TemplateVersionTreeDto.builder()
                .rootId(root.getId())
                .currentId(current.getId())
                .totalVersions(flatList.size())
                .tree(tree)
                .flatHistory(flatList)
                .build();
    }

    private TemplateVersionDto buildVersionNode(ReportTemplate entity, UUID currentId, List<TemplateVersionDto> flatList) {
        boolean isCurrent = entity.getId().equals(currentId);

        List<ReportTemplate> childrenEntities = repository.findByParentTemplate_Id(entity.getId());
        List<TemplateVersionDto> childrenDtos = new ArrayList<>();

        TemplateVersionDto dto = TemplateVersionDto.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .version(entity.getVersion())
                .statut(entity.getStatut())
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification())
                .parentTemplateId(entity.getParentTemplate() != null ? entity.getParentTemplate().getId() : null)
                .isCurrent(isCurrent)
                .children(childrenDtos)
                .build();

        flatList.add(dto);

        for (ReportTemplate child : childrenEntities) {
            childrenDtos.add(buildVersionNode(child, currentId, flatList));
        }

        return dto;
    }
}