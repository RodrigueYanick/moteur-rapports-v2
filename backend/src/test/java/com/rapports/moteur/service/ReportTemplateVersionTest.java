package com.rapports.moteur.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.dtoTemplate.TemplateVersionDto;
import com.rapports.moteur.dto.dtoTemplate.TemplateVersionTreeDto;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.entity.TemplateStatus;
import com.rapports.moteur.mapper.TemplateMapper;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.repository.ReportVariableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportTemplateVersionTest {

    @Mock
    private ReportTemplateRepository repository;

    @Mock
    private TemplateMapper mapper;

    @Mock
    private SchemaExtractorService schemaExtractorService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ReportVariableRepository variableRepository;

    @Mock
    private EntrepriseService entrepriseService;

    @Mock
    private CompanyWorkspaceConfigService workspaceConfigService;

    @InjectMocks
    private ReportTemplateService templateService;

    private UUID rootId;
    private UUID v2Id;
    private UUID v3Id;
    private ReportTemplate rootTemplate;
    private ReportTemplate v2Template;
    private ReportTemplate v3Template;

    @BeforeEach
    void setUp() {
        rootId = UUID.randomUUID();
        v2Id = UUID.randomUUID();
        v3Id = UUID.randomUUID();

        LocalDateTime now = LocalDateTime.now();

        rootTemplate = ReportTemplate.builder()
                .id(rootId)
                .nom("Facture V1")
                .version(1)
                .statut(TemplateStatus.ARCHIVE)
                .codeEntreprise("ENT1")
                .dateCreation(now.minusDays(5))
                .dateModification(now.minusDays(5))
                .parentTemplate(null)
                .build();

        v2Template = ReportTemplate.builder()
                .id(v2Id)
                .nom("Facture V2")
                .version(2)
                .statut(TemplateStatus.ARCHIVE)
                .codeEntreprise("ENT1")
                .dateCreation(now.minusDays(2))
                .dateModification(now.minusDays(2))
                .parentTemplate(rootTemplate)
                .build();

        v3Template = ReportTemplate.builder()
                .id(v3Id)
                .nom("Facture V3")
                .version(3)
                .statut(TemplateStatus.PUBLIE)
                .codeEntreprise("ENT1")
                .dateCreation(now)
                .dateModification(now)
                .parentTemplate(v2Template)
                .build();
    }

    @Test
    void testGetVersionTree_AscendingAndDescendingReconstruction() {
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn("ENT1");
        // On interroge depuis la V2
        when(repository.findById(v2Id)).thenReturn(Optional.of(v2Template));

        // Enfants de root
        when(repository.findByParentTemplate_Id(rootId)).thenReturn(List.of(v2Template));
        // Enfants de v2
        when(repository.findByParentTemplate_Id(v2Id)).thenReturn(List.of(v3Template));
        // Enfants de v3
        when(repository.findByParentTemplate_Id(v3Id)).thenReturn(List.of());

        TemplateVersionTreeDto result = templateService.getVersionTree(v2Id);

        assertNotNull(result);
        assertEquals(rootId, result.getRootId());
        assertEquals(v2Id, result.getCurrentId());
        assertEquals(3, result.getTotalVersions());

        // Vérification de l'arbre hiérarchique
        TemplateVersionDto treeRoot = result.getTree();
        assertEquals(rootId, treeRoot.getId());
        assertEquals(1, treeRoot.getVersion());
        assertFalse(treeRoot.isCurrent());
        assertEquals(1, treeRoot.getChildren().size());

        TemplateVersionDto treeV2 = treeRoot.getChildren().get(0);
        assertEquals(v2Id, treeV2.getId());
        assertEquals(2, treeV2.getVersion());
        assertTrue(treeV2.isCurrent()); // C'est la version consultée !
        assertEquals(1, treeV2.getChildren().size());

        TemplateVersionDto treeV3 = treeV2.getChildren().get(0);
        assertEquals(v3Id, treeV3.getId());
        assertEquals(3, treeV3.getVersion());
        assertFalse(treeV3.isCurrent());
        assertTrue(treeV3.getChildren().isEmpty());

        // Vérification de la liste chronologique plate
        List<TemplateVersionDto> flat = result.getFlatHistory();
        assertEquals(3, flat.size());
        assertEquals(1, flat.get(0).getVersion());
        assertEquals(2, flat.get(1).getVersion());
        assertEquals(3, flat.get(2).getVersion());
    }
}

