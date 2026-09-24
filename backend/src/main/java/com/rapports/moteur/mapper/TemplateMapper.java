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
                .parentTemplateId(entity.getParentTemplate() != null ? entity.getParentTemplate().getId() : null)
                .couleurFond(entity.getCouleurFond())
                .headerActif(entity.getHeaderActif())
                .hauteurHeaderMm(entity.getHauteurHeaderMm())
                .headerContenu(entity.getHeaderContenu())
                .headerAlignement(entity.getHeaderAlignement())
                .headerAfficherSurPremierePage(entity.getHeaderAfficherSurPremierePage())
                .headerLigneSeparation(entity.getHeaderLigneSeparation())
                .headerCouleurLigne(entity.getHeaderCouleurLigne())
                .footerActif(entity.getFooterActif())
                .hauteurFooterMm(entity.getHauteurFooterMm())
                .footerContenu(entity.getFooterContenu())
                .footerAlignement(entity.getFooterAlignement())
                .footerAfficherSurPremierePage(entity.getFooterAfficherSurPremierePage())
                .footerLigneSeparation(entity.getFooterLigneSeparation())
                .footerCouleurLigne(entity.getFooterCouleurLigne())
                .numerotationPage(entity.getNumerotationPage())
                .formatNumerotation(entity.getFormatNumerotation())
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
        template.setMargeGaucheMm(create.getMargeGaucheMm() != null ? create.getMargeGaucheMm() : 10);
        template.setMargeDroiteMm(create.getMargeDroiteMm() != null ? create.getMargeDroiteMm() : 10);
        template.setMargeHautMm(create.getMargeHautMm() != null ? create.getMargeHautMm() : 10);
        template.setMargeBasMm(create.getMargeBasMm() != null ? create.getMargeBasMm() : 10);
        template.setCouleurFond(create.getCouleurFond() != null ? create.getCouleurFond() : "#ffffff");
        template.setHeaderActif(create.getHeaderActif() != null ? create.getHeaderActif() : false);
        template.setHauteurHeaderMm(create.getHauteurHeaderMm() != null ? create.getHauteurHeaderMm() : 15);
        template.setHeaderContenu(create.getHeaderContenu());
        template.setHeaderAlignement(create.getHeaderAlignement() != null ? create.getHeaderAlignement() : "LEFT");
        template.setHeaderAfficherSurPremierePage(create.getHeaderAfficherSurPremierePage() != null ? create.getHeaderAfficherSurPremierePage() : true);
        template.setHeaderLigneSeparation(create.getHeaderLigneSeparation() != null ? create.getHeaderLigneSeparation() : false);
        template.setHeaderCouleurLigne(create.getHeaderCouleurLigne() != null ? create.getHeaderCouleurLigne() : "#d1d5db");
        template.setFooterActif(create.getFooterActif() != null ? create.getFooterActif() : false);
        template.setHauteurFooterMm(create.getHauteurFooterMm() != null ? create.getHauteurFooterMm() : 15);
        template.setFooterContenu(create.getFooterContenu());
        template.setFooterAlignement(create.getFooterAlignement() != null ? create.getFooterAlignement() : "LEFT");
        template.setFooterAfficherSurPremierePage(create.getFooterAfficherSurPremierePage() != null ? create.getFooterAfficherSurPremierePage() : true);
        template.setFooterLigneSeparation(create.getFooterLigneSeparation() != null ? create.getFooterLigneSeparation() : false);
        template.setFooterCouleurLigne(create.getFooterCouleurLigne() != null ? create.getFooterCouleurLigne() : "#d1d5db");
        template.setNumerotationPage(create.getNumerotationPage() != null ? create.getNumerotationPage() : true);
        template.setFormatNumerotation(create.getFormatNumerotation() != null ? create.getFormatNumerotation() : "PAGE_X_SUR_Y");
        return template;
    }
}