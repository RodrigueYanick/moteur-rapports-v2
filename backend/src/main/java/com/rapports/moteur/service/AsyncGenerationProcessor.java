package com.rapports.moteur.service;

import com.rapports.moteur.entity.GenerationStatus;
import com.rapports.moteur.entity.ReportGeneration;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.exceptions.TemplateNotFoundException;
import com.rapports.moteur.repository.ReportGenerationRepository;
import com.rapports.moteur.repository.ReportTemplateRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@Service
public class AsyncGenerationProcessor {

    private final ReportGenerationRepository generationRepository;
    private final ReportTemplateRepository templateRepository;
    private final TemplateHtmlBuilder htmlBuilder;
    private final PdfRendererService pdfRenderer;
    private final EntrepriseService entrepriseService;

    public AsyncGenerationProcessor(ReportGenerationRepository generationRepository,
                                    ReportTemplateRepository templateRepository,
                                    TemplateHtmlBuilder htmlBuilder,
                                    PdfRendererService pdfRenderer,
                                    EntrepriseService entrepriseService) {   // ✅ ajouté
        this.generationRepository = generationRepository;
        this.templateRepository = templateRepository;
        this.htmlBuilder = htmlBuilder;
        this.pdfRenderer = pdfRenderer;
        this.entrepriseService = entrepriseService;
    }

    @Async("generationExecutor")
    public void processAsync(@NonNull UUID generationId,
                             @NonNull UUID templateId,
                             Map<String, Object> data,
                             String storagePath) {
        ReportGeneration generation = generationRepository.findById(generationId)
                .orElseThrow(() -> new IllegalStateException("Generation introuvable : " + generationId));
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalStateException("Template introuvable : " + templateId));

        // ✅ Vérification d’appartenance à l’entreprise courante
        String currentCode = entrepriseService.getCurrentCodeEntreprise();
        if (currentCode == null || !currentCode.equals(template.getCodeEntreprise())) {
            // On ne révèle pas l’existence du template
            throw new TemplateNotFoundException("Template introuvable : " + templateId);
        }

        try {
            byte[] pdf = pdfRenderer.renderToPdf(
                htmlBuilder.build(
                    template.getContenuDesign(),
                    data,
                    template.getFormatPapier(),
                    template.getLargeurMm(),
                    template.getHauteurMm(),
                    template.getModePagination(),
                    template.getMargeGaucheMm(),
                    template.getMargeDroiteMm(),
                    template.getMargeHautMm(),
                    template.getMargeBasMm()
                )
            );
            String filePath = storePdf(generationId, pdf, storagePath);
            generation.setUrlFichierGenere(filePath);
            generation.setStatut(GenerationStatus.SUCCES);
        } catch (Exception e) {
            generation.setStatut(GenerationStatus.ECHEC);
        }
        generationRepository.save(generation);
    }

    private String storePdf(UUID generationId, byte[] pdf, String storagePath) {
        try {
            Path dir = Paths.get(storagePath);
            Files.createDirectories(dir);
            Path file = dir.resolve(generationId + ".pdf");
            Files.write(file, pdf);
            return file.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de stocker le PDF", e);
        }
    }
}