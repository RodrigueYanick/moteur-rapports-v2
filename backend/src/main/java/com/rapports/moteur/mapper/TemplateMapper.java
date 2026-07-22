package com.rapports.moteur.mapper;

import org.springframework.stereotype.Component;

import com.rapports.moteur.dto.dtoTemplate.TemplateCreate;
import com.rapports.moteur.dto.dtoTemplate.TemplateResponse;
import com.rapports.moteur.entity.ReportTemplate;

@Component
public class TemplateMapper {
    
    public TemplateResponse toDto(ReportTemplate entity) {
        return TemplateResponse.builder()
                .id(entity.getId())
                .nom(entity.getNom())
                .description(entity.getDescription())
                .contenuDesign(entity.getContenuDesign())
                .statut(entity.getStatut())
                .version(entity.getVersion())
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification())
                .build();
    }

    public ReportTemplate toEntity(TemplateCreate create){
        ReportTemplate template = new ReportTemplate();
        template.setNom(create.getNom());
        template.setContenuDesign(create.getContenuDesign());
        template.setDescription(create.getDescription());
        return template;
    }
}