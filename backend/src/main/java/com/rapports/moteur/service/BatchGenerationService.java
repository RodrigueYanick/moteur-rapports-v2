package com.rapports.moteur.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.dtoBatch.*;
import com.rapports.moteur.entity.*;
import com.rapports.moteur.exceptions.TemplateNotFoundException;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.mapper.BatchMapper;
import com.rapports.moteur.repository.BatchGenerationItemRepository;
import com.rapports.moteur.repository.ReportBatchRepository;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchGenerationService {

    private final ReportBatchRepository batchRepository;
    private final BatchGenerationItemRepository itemRepository;
    private final ReportTemplateRepository templateRepository;
    private final EntrepriseService entrepriseService;
    private final AsyncBatchProcessor asyncBatchProcessor;
    private final BatchMapper batchMapper;
    private final FileStorageService fileStorageService;
    private final com.rapports.moteur.security.UrlSecurityValidator urlSecurityValidator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Crée et démarre immédiatement un lot de génération asynchrone.
     */
    @Transactional
    public BatchResponse createAndStartBatch(UUID templateId, BatchCreateRequest request) {
        if (templateId == null && request.getTemplateId() != null) {
            templateId = request.getTemplateId();
        }
        if (templateId == null) {
            throw new ValidationException(List.of("L'identifiant du template (templateId) est obligatoire"));
        }

        if (request.getWebhookUrl() != null && !request.getWebhookUrl().isBlank()) {
            urlSecurityValidator.validateSafeUrl(request.getWebhookUrl());
        }

        ReportTemplate template = loadTemplateForCurrentEntreprise(templateId);
        if (template.getStatut() != TemplateStatus.PUBLIE) {
            throw new ValidationException(List.of("Le modèle doit être publié pour pouvoir générer des rapports par lot"));
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ValidationException(List.of("Le lot doit contenir au moins un élément à générer"));
        }

        String currentCode = entrepriseService.getCurrentCodeEntreprise();
        String batchCodeEntreprise = (template.getCodeEntreprise() != null && !template.getCodeEntreprise().isBlank())
                ? template.getCodeEntreprise()
                : currentCode;

        if (batchCodeEntreprise == null || batchCodeEntreprise.isBlank()) {
            throw new ValidationException(List.of("Un code entreprise est requis pour lancer une génération par lot"));
        }

        ReportBatch batch = ReportBatch.builder()
                .template(template)
                .codeEntreprise(batchCodeEntreprise)
                .statut(BatchStatus.EN_ATTENTE)
                .totalItems(request.getItems().size())
                .processedItems(0)
                .successCount(0)
                .failureCount(0)
                .webhookUrl(request.getWebhookUrl() != null ? request.getWebhookUrl().trim() : null)
                .webhookSecret(request.getWebhookSecret() != null ? request.getWebhookSecret().trim() : null)
                .webhookStatut((request.getWebhookUrl() != null && !request.getWebhookUrl().isBlank())
                        ? WebhookStatus.EN_ATTENTE
                        : WebhookStatus.NON_CONFIGURE)
                .webhookTentatives(0)
                .dateCreation(LocalDateTime.now())
                .build();

        ReportBatch savedBatch = batchRepository.save(batch);

        List<BatchGenerationItem> items = new ArrayList<>();
        for (BatchItemRequest itemReq : request.getItems()) {
            String donneesJson;
            try {
                donneesJson = objectMapper.writeValueAsString(itemReq.getData() != null ? itemReq.getData() : Collections.emptyMap());
            } catch (Exception e) {
                donneesJson = "{}";
            }

            BatchGenerationItem item = BatchGenerationItem.builder()
                    .batch(savedBatch)
                    .customId(itemReq.getCustomId())
                    .statut(BatchItemStatus.EN_ATTENTE)
                    .donnees(donneesJson)
                    .build();
            items.add(item);
        }

        itemRepository.saveAll(items);
        savedBatch.setItems(items);

        // Lancement asynchrone du traitement
        asyncBatchProcessor.processBatchAsync(savedBatch.getId(), false);

        return batchMapper.toResponse(savedBatch, true);
    }

    /**
     * Récupère l'état et la progression d'un lot avec ses éléments.
     */
    public BatchResponse getBatch(UUID batchId) {
        ReportBatch batch = findAndVerifyBatch(batchId);
        return batchMapper.toResponse(batch, true);
    }

    /**
     * Récupère la liste des éléments d'un lot.
     */
    public List<BatchItemResponse> getBatchItems(UUID batchId) {
        findAndVerifyBatch(batchId);
        return itemRepository.findByBatch_IdOrderByDateTraitementAsc(batchId).stream()
                .map(batchMapper::toItemResponse)
                .toList();
    }

    /**
     * Liste les lots accessibles pour l'entreprise courante ou pour un template spécifique.
     */
    public List<BatchResponse> listBatches(UUID templateId) {
        if (templateId != null) {
            loadTemplateForCurrentEntreprise(templateId);
            return batchRepository.findByTemplate_IdOrderByDateCreationDesc(templateId).stream()
                    .map(b -> batchMapper.toResponse(b, false))
                    .toList();
        }

        String currentCode = entrepriseService.getCurrentCodeEntreprise();
        if (currentCode == null || currentCode.isBlank()) {
            return Collections.emptyList();
        }
        return batchRepository.findAllByEntreprise(currentCode).stream()
                .map(b -> batchMapper.toResponse(b, false))
                .toList();
    }

    /**
     * Relance uniquement les éléments d'un lot qui ont échoué.
     */
    @Transactional
    public BatchResponse retryFailedItems(UUID batchId) {
        ReportBatch batch = findAndVerifyBatch(batchId);

        if (batch.getStatut() == BatchStatus.EN_COURS) {
            throw new IllegalStateException("Un traitement est déjà en cours sur ce lot");
        }

        List<BatchGenerationItem> failed = itemRepository.findByBatch_IdAndStatut(batchId, BatchItemStatus.ECHEC);
        if (failed.isEmpty()) {
            throw new IllegalStateException("Aucun élément en échec à relancer dans ce lot");
        }

        batch.setStatut(BatchStatus.EN_ATTENTE);
        batch.setDateFin(null);
        batchRepository.save(batch);

        asyncBatchProcessor.processBatchAsync(batchId, true);

        return batchMapper.toResponse(batch, true);
    }

    /**
     * Compile tous les PDFs générés avec succès dans une archive ZIP téléchargeable.
     */
    public byte[] generateZipArchive(UUID batchId) {
        ReportBatch batch = findAndVerifyBatch(batchId);

        List<BatchGenerationItem> successfulItems = itemRepository.findByBatch_IdAndStatut(batchId, BatchItemStatus.SUCCES);
        if (successfulItems.isEmpty()) {
            throw new IllegalStateException("Aucun rapport généré avec succès dans ce lot pour constituer une archive ZIP");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            Set<String> usedNames = new HashSet<>();
            int index = 1;

            for (BatchGenerationItem item : successfulItems) {
                if (item.getUrlFichier() == null || item.getUrlFichier().isBlank()) {
                    continue;
                }

                byte[] pdfBytes;
                try {
                    pdfBytes = fileStorageService.loadFile(item.getUrlFichier());
                } catch (Exception e) {
                    log.warn("Impossible de charger le fichier {} pour le ZIP du lot {}", item.getUrlFichier(), batchId, e);
                    continue;
                }

                String filename = sanitizeFilename(item.getCustomId(), index, item.getId());
                while (usedNames.contains(filename.toLowerCase())) {
                    filename = "item_" + index + "_" + filename;
                }
                usedNames.add(filename.toLowerCase());

                ZipEntry entry = new ZipEntry(filename);
                zos.putNextEntry(entry);
                zos.write(pdfBytes);
                zos.closeEntry();

                index++;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Erreur lors de la compression de l'archive ZIP : " + e.getMessage(), e);
        }

        return baos.toByteArray();
    }

    private ReportBatch findAndVerifyBatch(UUID batchId) {
        ReportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalStateException("Lot introuvable : " + batchId));

        String currentCode = entrepriseService.getCurrentCodeEntreprise();
        if (batch.getCodeEntreprise() != null && !batch.getCodeEntreprise().isBlank()) {
            if (currentCode == null || !currentCode.equals(batch.getCodeEntreprise())) {
                throw new IllegalStateException("Lot introuvable : " + batchId);
            }
        }
        return batch;
    }

    private ReportTemplate loadTemplateForCurrentEntreprise(UUID id) {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + id));

        String currentCode = entrepriseService.getCurrentCodeEntreprise();
        if (template.getCodeEntreprise() != null && !template.getCodeEntreprise().isBlank()) {
            if (currentCode == null || !currentCode.equals(template.getCodeEntreprise())) {
                throw new TemplateNotFoundException("Template introuvable : " + id);
            }
        }
        return template;
    }

    private String sanitizeFilename(String customId, int index, UUID itemId) {
        if (customId != null && !customId.isBlank()) {
            String clean = customId.replaceAll("[^a-zA-Z0-9._-]", "_");
            return clean.endsWith(".pdf") ? clean : clean + ".pdf";
        }
        return "rapport_" + index + "_" + itemId.toString().substring(0, 8) + ".pdf";
    }
}

