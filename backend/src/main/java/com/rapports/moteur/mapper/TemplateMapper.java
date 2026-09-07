package com.rapports.moteur.mapper;

import org.springframework.stereotype.Component;

import com.rapports.moteur.dto.dtoTemplate.TemplateCreate;
import com.rapports.moteur.dto.dtoTemplate.TemplateResponse;
import com.rapports.moteur.entity.PaginationMode;
import com.rapports.moteur.entity.ReportTemplate;

@Component
public class TemplateMapper {
    
    public TemplateResponse toDto(ReportTemplate entity) {
        return TemplateResponse.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .description(entity.getDescription())
                .contenuDesign(entity.getContenuDesign())
                .categorie(entity.getCategorie())
                .formatPapier(entity.getFormatPapier())
                .largeurMm(entity.getLargeurMm())
                .hauteurMm(entity.getHauteurMm())
                .statut(entity.getStatut())
                .version(entity.getVersion())
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification())
                .modePagination(entity.getModePagination())
                .margeGaucheMm(entity.getMargeGaucheMm())
                .margeDroiteMm(entity.getMargeDroiteMm())
                .margeHautMm(entity.getMargeHautMm())
                .margeBasMm(entity.getMargeBasMm())
                .build();
    }

    public ReportTemplate toEntity(TemplateCreate create) {
        ReportTemplate template = new ReportTemplate();
        template.setNom(create.getNom());
        template.setContenuDesign(create.getContenuDesign());
        template.setDescription(create.getDescription());
        template.setCategorie(create.getCategorie());
        template.setFormatPapier(create.getFormatPapier());
        template.setLargeurMm(create.getLargeurMm());
        template.setHauteurMm(create.getHauteurMm());
        template.setModePagination(create.getModePagination() != null ? create.getModePagination() : PaginationMode.FIXED);
        template.setMargeGaucheMm(create.getMargeGaucheMm() != null ? create.getMargeGaucheMm() : 0);
        template.setMargeDroiteMm(create.getMargeDroiteMm() != null ? create.getMargeDroiteMm() : 0);
        template.setMargeHautMm(create.getMargeHautMm() != null ? create.getMargeHautMm() : 0);
        template.setMargeBasMm(create.getMargeBasMm() != null ? create.getMargeBasMm() : 0);
        return template;
    }
}