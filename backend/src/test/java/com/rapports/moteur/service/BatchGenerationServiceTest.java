package com.rapports.moteur.service;

import com.rapports.moteur.dto.dtoBatch.*;
import com.rapports.moteur.entity.*;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.mapper.BatchMapper;
import com.rapports.moteur.repository.BatchGenerationItemRepository;
import com.rapports.moteur.repository.ReportBatchRepository;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.security.UrlSecurityValidator;
import com.rapports.moteur.service.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchGenerationServiceTest {

    @Mock
    private ReportBatchRepository batchRepository;

    @Mock
    private BatchGenerationItemRepository itemRepository;

    @Mock
    private ReportTemplateRepository templateRepository;

    @Mock
    private EntrepriseService entrepriseService;

    @Mock
    private AsyncBatchProcessor asyncBatchProcessor;

    @Mock
    private BatchMapper batchMapper;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private UrlSecurityValidator urlSecurityValidator;

    @InjectMocks
    private BatchGenerationService batchService;

    private UUID templateId;
    private ReportTemplate template;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        template = ReportTemplate.builder()
                .id(templateId)
                .nom("Facture Vente")
                .statut(TemplateStatus.PUBLIE)
                .codeEntreprise("ENT-001")
                .build();
    }

    @Test
    @DisplayName("createAndStartBatch crée le lot et déclenche le traitement asynchrone")
    void testCreateAndStartBatchSuccess() {
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn("ENT-001");

        ReportBatch savedBatch = ReportBatch.builder()
                .id(UUID.randomUUID())
                .template(template)
                .codeEntreprise("ENT-001")
                .statut(BatchStatus.EN_ATTENTE)
                .totalItems(2)
                .webhookUrl("https://example.com/webhook")
                .dateCreation(LocalDateTime.now())
                .build();

        when(batchRepository.save(any(ReportBatch.class))).thenReturn(savedBatch);
        when(batchMapper.toResponse(any(ReportBatch.class), eq(true))).thenReturn(
                BatchResponse.builder()
                        .id(savedBatch.getId())
                        .templateId(templateId)
                        .totalItems(2)
                        .statut(BatchStatus.EN_ATTENTE)
                        .build()
        );

        BatchCreateRequest request = BatchCreateRequest.builder()
                .items(List.of(
                        BatchItemRequest.builder().customId("FAC-001").data(Map.of("montant", 100)).build(),
                        BatchItemRequest.builder().customId("FAC-002").data(Map.of("montant", 200)).build()
                ))
                .webhookUrl("https://example.com/webhook")
                .build();

        BatchResponse response = batchService.createAndStartBatch(templateId, request);

        assertThat(response).isNotNull();
        assertThat(response.getTotalItems()).isEqualTo(2);
        assertThat(response.getStatut()).isEqualTo(BatchStatus.EN_ATTENTE);

        verify(itemRepository).saveAll(anyList());
        verify(asyncBatchProcessor).processBatchAsync(savedBatch.getId(), false);
    }

    @Test
    @DisplayName("createAndStartBatch refuse un template non publié")
    void testCreateAndStartBatchBrouillonRefused() {
        template.setStatut(TemplateStatus.BROUILLON);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn("ENT-001");

        BatchCreateRequest request = BatchCreateRequest.builder()
                .items(List.of(BatchItemRequest.builder().customId("FAC-001").data(Map.of()).build()))
                .build();

        assertThatThrownBy(() -> batchService.createAndStartBatch(templateId, request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("publié");

        verifyNoInteractions(batchRepository);
        verifyNoInteractions(asyncBatchProcessor);
    }

    @Test
    @DisplayName("generateZipArchive génère une archive ZIP valide contenant les rapports PDF")
    void testGenerateZipArchiveSuccess() throws Exception {
        UUID batchId = UUID.randomUUID();
        ReportBatch batch = ReportBatch.builder()
                .id(batchId)
                .codeEntreprise("ENT-001")
                .statut(BatchStatus.TERMINE)
                .build();

        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn("ENT-001");

        byte[] fakePdf1 = "%PDF-1.4 Fake 1".getBytes(StandardCharsets.UTF_8);
        byte[] fakePdf2 = "%PDF-1.4 Fake 2".getBytes(StandardCharsets.UTF_8);

        BatchGenerationItem item1 = BatchGenerationItem.builder()
                .id(UUID.randomUUID())
                .batch(batch)
                .customId("facture_janvier")
                .statut(BatchItemStatus.SUCCES)
                .urlFichier("path/to/pdf1.pdf")
                .build();

        BatchGenerationItem item2 = BatchGenerationItem.builder()
                .id(UUID.randomUUID())
                .batch(batch)
                .customId("facture_fevrier")
                .statut(BatchItemStatus.SUCCES)
                .urlFichier("path/to/pdf2.pdf")
                .build();

        when(itemRepository.findByBatch_IdAndStatut(batchId, BatchItemStatus.SUCCES))
                .thenReturn(List.of(item1, item2));
        when(fileStorageService.loadFile("path/to/pdf1.pdf")).thenReturn(fakePdf1);
        when(fileStorageService.loadFile("path/to/pdf2.pdf")).thenReturn(fakePdf2);

        byte[] zipBytes = batchService.generateZipArchive(batchId);

        assertThat(zipBytes).isNotEmpty();

        // Validation du contenu du fichier ZIP
        List<String> entryNames = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNames.add(entry.getName());
                zis.closeEntry();
            }
        }

        assertThat(entryNames).containsExactlyInAnyOrder("facture_janvier.pdf", "facture_fevrier.pdf");
    }

    @Test
    @DisplayName("retryFailedItems refuse si aucun élément n'est en échec")
    void testRetryFailedItemsNone() {
        UUID batchId = UUID.randomUUID();
        ReportBatch batch = ReportBatch.builder()
                .id(batchId)
                .codeEntreprise("ENT-001")
                .statut(BatchStatus.TERMINE)
                .build();

        when(batchRepository.findById(batchId)).thenReturn(Optional.of(batch));
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn("ENT-001");
        when(itemRepository.findByBatch_IdAndStatut(batchId, BatchItemStatus.ECHEC))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> batchService.retryFailedItems(batchId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Aucun élément en échec");
    }
}

