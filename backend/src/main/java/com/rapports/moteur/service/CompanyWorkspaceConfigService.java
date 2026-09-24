package com.rapports.moteur.service;

import com.rapports.moteur.dto.dtoWorkspace.WorkspaceConfigRequest;
import com.rapports.moteur.dto.dtoWorkspace.WorkspaceConfigResponse;
import com.rapports.moteur.entity.CompanyWorkspaceConfig;
import com.rapports.moteur.entity.PaginationMode;
import com.rapports.moteur.repository.CompanyWorkspaceConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyWorkspaceConfigService {

    private final CompanyWorkspaceConfigRepository repository;
    private final EntrepriseService entrepriseService;

    private String resolveCode(String code) {
        return (code == null || code.isBlank()) ? "DEFAULT" : code;
    }

    public WorkspaceConfigResponse getConfigForCurrentEntreprise() {
        final String code = resolveCode(entrepriseService.getCurrentCodeEntreprise());
        return repository.findByCodeEntreprise(code)
                .map(this::toResponse)
                .orElseGet(() -> buildDefaultResponse(code));
    }

    public CompanyWorkspaceConfig getEntityForCurrentEntreprise() {
        final String code = resolveCode(entrepriseService.getCurrentCodeEntreprise());
        return repository.findByCodeEntreprise(code)
                .orElseGet(() -> buildDefaultEntity(code));
    }

    @Transactional
    public WorkspaceConfigResponse saveOrUpdateConfig(WorkspaceConfigRequest request) {
        final String code = resolveCode(entrepriseService.getCurrentCodeEntreprise());

        CompanyWorkspaceConfig entity = repository.findByCodeEntreprise(code)
                .orElseGet(() -> {
                    CompanyWorkspaceConfig c = new CompanyWorkspaceConfig();
                    c.setCodeEntreprise(code);
                    return c;
                });

        if (request.getFormatPapier() != null) entity.setFormatPapier(request.getFormatPapier());
        entity.setLargeurMm(request.getLargeurMm());
        entity.setHauteurMm(request.getHauteurMm());
        if (request.getModePagination() != null) entity.setModePagination(request.getModePagination());
        if (request.getMargeGaucheMm() != null) entity.setMargeGaucheMm(request.getMargeGaucheMm());
        if (request.getMargeDroiteMm() != null) entity.setMargeDroiteMm(request.getMargeDroiteMm());
        if (request.getMargeHautMm() != null) entity.setMargeHautMm(request.getMargeHautMm());
        if (request.getMargeBasMm() != null) entity.setMargeBasMm(request.getMargeBasMm());
        if (request.getCouleurFond() != null) entity.setCouleurFond(request.getCouleurFond());

        // Header
        if (request.getHeaderActif() != null) entity.setHeaderActif(request.getHeaderActif());
        if (request.getHauteurHeaderMm() != null) entity.setHauteurHeaderMm(request.getHauteurHeaderMm());
        entity.setHeaderContenu(request.getHeaderContenu());
        if (request.getHeaderAlignement() != null) entity.setHeaderAlignement(request.getHeaderAlignement());
        if (request.getHeaderAfficherSurPremierePage() != null) entity.setHeaderAfficherSurPremierePage(request.getHeaderAfficherSurPremierePage());
        if (request.getHeaderLigneSeparation() != null) entity.setHeaderLigneSeparation(request.getHeaderLigneSeparation());
        if (request.getHeaderCouleurLigne() != null) entity.setHeaderCouleurLigne(request.getHeaderCouleurLigne());

        // Footer
        if (request.getFooterActif() != null) entity.setFooterActif(request.getFooterActif());
        if (request.getHauteurFooterMm() != null) entity.setHauteurFooterMm(request.getHauteurFooterMm());
        entity.setFooterContenu(request.getFooterContenu());
        if (request.getFooterAlignement() != null) entity.setFooterAlignement(request.getFooterAlignement());
        if (request.getFooterAfficherSurPremierePage() != null) entity.setFooterAfficherSurPremierePage(request.getFooterAfficherSurPremierePage());
        if (request.getFooterLigneSeparation() != null) entity.setFooterLigneSeparation(request.getFooterLigneSeparation());
        if (request.getFooterCouleurLigne() != null) entity.setFooterCouleurLigne(request.getFooterCouleurLigne());
        if (request.getNumerotationPage() != null) entity.setNumerotationPage(request.getNumerotationPage());
        if (request.getFormatNumerotation() != null) entity.setFormatNumerotation(request.getFormatNumerotation());

        CompanyWorkspaceConfig saved = repository.save(entity);
        return toResponse(saved);
    }

    @Transactional
    public WorkspaceConfigResponse resetToDefaults() {
        final String code = resolveCode(entrepriseService.getCurrentCodeEntreprise());

        CompanyWorkspaceConfig entity = repository.findByCodeEntreprise(code)
                .orElseGet(() -> {
                    CompanyWorkspaceConfig c = new CompanyWorkspaceConfig();
                    c.setCodeEntreprise(code);
                    return c;
                });

        entity.setFormatPapier("A4");
        entity.setLargeurMm(null);
        entity.setHauteurMm(null);
        entity.setModePagination(PaginationMode.FIXED);
        entity.setMargeGaucheMm(10);
        entity.setMargeDroiteMm(10);
        entity.setMargeHautMm(10);
        entity.setMargeBasMm(10);
        entity.setCouleurFond("#ffffff");

        entity.setHeaderActif(false);
        entity.setHauteurHeaderMm(15);
        entity.setHeaderContenu("");
        entity.setHeaderAlignement("LEFT");
        entity.setHeaderAfficherSurPremierePage(true);
        entity.setHeaderLigneSeparation(false);
        entity.setHeaderCouleurLigne("#d1d5db");

        entity.setFooterActif(false);
        entity.setHauteurFooterMm(15);
        entity.setFooterContenu("");
        entity.setFooterAlignement("LEFT");
        entity.setFooterAfficherSurPremierePage(true);
        entity.setFooterLigneSeparation(false);
        entity.setFooterCouleurLigne("#d1d5db");
        entity.setNumerotationPage(true);
        entity.setFormatNumerotation("PAGE_X_SUR_Y");

        CompanyWorkspaceConfig saved = repository.save(entity);
        return toResponse(saved);
    }

    private CompanyWorkspaceConfig buildDefaultEntity(String codeEntreprise) {
        return CompanyWorkspaceConfig.builder()
                .codeEntreprise(codeEntreprise)
                .formatPapier("A4")
                .modePagination(PaginationMode.FIXED)
                .margeGaucheMm(10)
                .margeDroiteMm(10)
                .margeHautMm(10)
                .margeBasMm(10)
                .couleurFond("#ffffff")
                .headerActif(false)
                .hauteurHeaderMm(15)
                .headerAlignement("LEFT")
                .headerAfficherSurPremierePage(true)
                .headerLigneSeparation(false)
                .headerCouleurLigne("#d1d5db")
                .footerActif(false)
                .hauteurFooterMm(15)
                .footerAlignement("LEFT")
                .footerAfficherSurPremierePage(true)
                .footerLigneSeparation(false)
                .footerCouleurLigne("#d1d5db")
                .numerotationPage(true)
                .formatNumerotation("PAGE_X_SUR_Y")
                .build();
    }

    private WorkspaceConfigResponse buildDefaultResponse(String codeEntreprise) {
        return WorkspaceConfigResponse.builder()
                .codeEntreprise(codeEntreprise)
                .formatPapier("A4")
                .modePagination(PaginationMode.FIXED)
                .margeGaucheMm(10)
                .margeDroiteMm(10)
                .margeHautMm(10)
                .margeBasMm(10)
                .couleurFond("#ffffff")
                .headerActif(false)
                .hauteurHeaderMm(15)
                .headerContenu("")
                .headerAlignement("LEFT")
                .headerAfficherSurPremierePage(true)
                .headerLigneSeparation(false)
                .headerCouleurLigne("#d1d5db")
                .footerActif(false)
                .hauteurFooterMm(15)
                .footerContenu("")
                .footerAlignement("LEFT")
                .footerAfficherSurPremierePage(true)
                .footerLigneSeparation(false)
                .footerCouleurLigne("#d1d5db")
                .numerotationPage(true)
                .formatNumerotation("PAGE_X_SUR_Y")
                .build();
    }

    private WorkspaceConfigResponse toResponse(CompanyWorkspaceConfig entity) {
        return WorkspaceConfigResponse.builder()
                .id(entity.getId())
                .codeEntreprise(entity.getCodeEntreprise())
                .formatPapier(entity.getFormatPapier())
                .largeurMm(entity.getLargeurMm())
                .hauteurMm(entity.getHauteurMm())
                .modePagination(entity.getModePagination())
                .margeGaucheMm(entity.getMargeGaucheMm())
                .margeDroiteMm(entity.getMargeDroiteMm())
                .margeHautMm(entity.getMargeHautMm())
                .margeBasMm(entity.getMargeBasMm())
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
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification())
                .build();
    }
}
