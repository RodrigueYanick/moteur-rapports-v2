package com.rapports.moteur.service;

import com.rapports.moteur.dto.dtoWorkspace.WorkspaceConfigRequest;
import com.rapports.moteur.dto.dtoWorkspace.WorkspaceConfigResponse;
import com.rapports.moteur.entity.CompanyWorkspaceConfig;
import com.rapports.moteur.entity.PaginationMode;
import com.rapports.moteur.repository.CompanyWorkspaceConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyWorkspaceConfigServiceTest {

    @Mock
    private CompanyWorkspaceConfigRepository repository;

    @Mock
    private EntrepriseService entrepriseService;

    @InjectMocks
    private CompanyWorkspaceConfigService service;

    @BeforeEach
    void setUp() {
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn("ENT-001");
    }

    @Test
    void testGetConfigWhenNotExistsReturnsDefaults() {
        when(repository.findByCodeEntreprise("ENT-001")).thenReturn(Optional.empty());

        WorkspaceConfigResponse res = service.getConfigForCurrentEntreprise();

        assertNotNull(res);
        assertEquals("ENT-001", res.getCodeEntreprise());
        assertEquals("A4", res.getFormatPapier());
        assertEquals(10, res.getMargeGaucheMm());
        assertEquals(10, res.getMargeDroiteMm());
        assertEquals(10, res.getMargeHautMm());
        assertEquals(10, res.getMargeBasMm());
        assertEquals("#ffffff", res.getCouleurFond());
        assertFalse(res.getHeaderActif());
        assertFalse(res.getFooterActif());
        assertTrue(res.getNumerotationPage());
    }

    @Test
    void testSaveOrUpdateConfig() {
        when(repository.findByCodeEntreprise("ENT-001")).thenReturn(Optional.empty());
        when(repository.save(any(CompanyWorkspaceConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceConfigRequest req = new WorkspaceConfigRequest();
        req.setFormatPapier("A3");
        req.setMargeHautMm(20);
        req.setMargeBasMm(25);
        req.setCouleurFond("#f0f4f8");
        req.setHeaderActif(true);
        req.setHeaderContenu("En-tête entreprise");
        req.setHeaderAlignement("CENTRE");
        req.setFooterActif(true);
        req.setFooterContenu("Confidentiel - Page {page}/{pages}");

        WorkspaceConfigResponse res = service.saveOrUpdateConfig(req);

        assertNotNull(res);
        assertEquals("ENT-001", res.getCodeEntreprise());
        assertEquals("A3", res.getFormatPapier());
        assertEquals(20, res.getMargeHautMm());
        assertEquals(25, res.getMargeBasMm());
        assertEquals("#f0f4f8", res.getCouleurFond());
        assertTrue(res.getHeaderActif());
        assertEquals("En-tête entreprise", res.getHeaderContenu());
        assertEquals("CENTRE", res.getHeaderAlignement());
        assertTrue(res.getFooterActif());
        assertEquals("Confidentiel - Page {page}/{pages}", res.getFooterContenu());
        verify(repository, times(1)).save(any(CompanyWorkspaceConfig.class));
    }

    @Test
    void testResetToDefaults() {
        CompanyWorkspaceConfig existing = new CompanyWorkspaceConfig();
        existing.setCodeEntreprise("ENT-001");
        existing.setFormatPapier("Letter");
        existing.setHeaderActif(true);

        when(repository.findByCodeEntreprise("ENT-001")).thenReturn(Optional.of(existing));
        when(repository.save(any(CompanyWorkspaceConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceConfigResponse res = service.resetToDefaults();

        assertNotNull(res);
        assertEquals("A4", res.getFormatPapier());
        assertFalse(res.getHeaderActif());
        assertFalse(res.getFooterActif());
        assertEquals(10, res.getMargeHautMm());
    }
}

