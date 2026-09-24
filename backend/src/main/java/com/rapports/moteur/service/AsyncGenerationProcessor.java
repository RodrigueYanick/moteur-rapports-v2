package com.rapports.moteur.service;

import com.rapports.moteur.entity.GenerationStatus;
import com.rapports.moteur.entity.ReportGeneration;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.exceptions.TemplateNotFoundException;
import com.rapports.moteur.repository.ReportGenerationRepository;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.service.rendering.RenderOptions;
import com.rapports.moteur.service.storage.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class AsyncGenerationProcessor {

    private final ReportGenerationRepository generationRepository;
    private final ReportTemplateRepository templateRepository;
    private final TemplateHtmlBuilder htmlBuilder;
    private final PdfRendererService pdfRenderer;
    private final EntrepriseService entrepriseService;
    private final FileStorageService fileStorageService;

    public AsyncGenerationProcessor(ReportGenerationRepository generationRepository,
                                    ReportTemplateRepository templateRepository,
                                    TemplateHtmlBuilder htmlBuilder,
                                    PdfRendererService pdfRenderer,
                                    EntrepriseService entrepriseService,
                                    FileStorageService fileStorageService) {
        this.generationRepository = generationRepository;
        this.templateRepository = templateRepository;
        this.htmlBuilder = htmlBuilder;
        this.pdfRenderer = pdfRenderer;
        this.entrepriseService = entrepriseService;
        this.fileStorageService = fileStorageService;
    }

    @Async("generationExecutor")
    public void processAsync(@NonNull UUID generationId,
                             @NonNull UUID templateId,
                             Map<String, Object> data) {
        ReportGeneration generation = generationRepository.findById(generationId)
                .orElseThrow(() -> new IllegalStateException("Generation introuvable : " + generationId));
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalStateException("Template introuvable : " + templateId));

        // ✅ Vérification d’appartenance à l’entreprise courante
        String currentCode = entrepriseService.getCurrentCodeEntreprise();
        if (template.getCodeEntreprise() != null && !template.getCodeEntreprise().isBlank()) {
            if (currentCode == null || !currentCode.equals(template.getCodeEntreprise())) {
                throw new TemplateNotFoundException("Template introuvable : " + templateId);
            }
        }

        try {
            RenderOptions options = RenderOptions.fromTemplate(template);
            byte[] pdf = pdfRenderer.renderToPdf(htmlBuilder.build(template, data), options);
            String storageKey = buildStorageKey(template, generationId);
            String savedPath = fileStorageService.storeFile(storageKey, pdf, "application/pdf");
            generation.setUrlFichierGenere(savedPath);
            generation.setStatut(GenerationStatus.SUCCES);
        } catch (Exception e) {
            log.error("Erreur lors de la génération asynchrone pour generationId={}", generationId, e);
            generation.setStatut(GenerationStatus.ECHEC);
        }
        generationRepository.save(generation);
    }

    @Async("generationExecutor")
    public void processAsync(@NonNull UUID generationId,
                             @NonNull UUID templateId,
                             Map<String, Object> data,
                             String storagePath) {
        processAsync(generationId, templateId, data);
    }

    private String buildStorageKey(ReportTemplate template, UUID generationId) {
        String codeEntreprise = template.getCodeEntreprise();
        if (codeEntreprise != null && !codeEntreprise.isBlank()) {
            return "entreprises/" + codeEntreprise.trim() + "/reports/" + generationId + ".pdf";
        }
        return "public/reports/" + generationId + ".pdf";
    }
}