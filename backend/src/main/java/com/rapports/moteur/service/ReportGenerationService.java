package com.rapports.moteur.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.config.AppProperties;
import com.rapports.moteur.dto.dtoGeneration.GenerationDto;
import com.rapports.moteur.dto.dtoGeneration.GenerationResponse;
import com.rapports.moteur.entity.*;
import com.rapports.moteur.exceptions.TemplateNotFoundException;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.mapper.GenerationMapper;
import com.rapports.moteur.repository.ReportGenerationRepository;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.repository.ReportVariableRepository;

import com.rapports.moteur.service.rendering.RenderOptions;
import com.rapports.moteur.service.storage.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReportGenerationService {

    private final ReportGenerationRepository generationRepository;
    private final ReportTemplateRepository templateRepository;
    private final DataValidatorService validatorService;
    private final TemplateHtmlBuilder htmlBuilder;
    private final PdfRendererService pdfRenderer;
    private final GenerationMapper generationMapper;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AsyncGenerationProcessor asyncProcessor;
    private final ReportVariableRepository variableRepository;
    private final EntrepriseService entrepriseService;
    private final FileStorageService fileStorageService;
    private final ExcelRendererService excelRenderer;
    private final com.rapports.moteur.service.facturx.FacturXPdfService facturXPdfService;
    private final com.rapports.moteur.service.metrics.ReportMetricsService reportMetricsService;

    public ReportGenerationService(ReportGenerationRepository generationRepository,
                                    ReportTemplateRepository templateRepository,
                                    DataValidatorService validatorService,
                                    TemplateHtmlBuilder htmlBuilder,
                                    PdfRendererService pdfRenderer,
                                    GenerationMapper generationMapper,
                                    AppProperties appProperties,
                                    ReportVariableRepository variableRepository,
                                    AsyncGenerationProcessor asyncProcessor,
                                    EntrepriseService entrepriseService,
                                    FileStorageService fileStorageService,
                                    ExcelRendererService excelRenderer,
                                    com.rapports.moteur.service.facturx.FacturXPdfService facturXPdfService,
                                    com.rapports.moteur.service.metrics.ReportMetricsService reportMetricsService) {
        this.generationRepository = generationRepository;
        this.templateRepository = templateRepository;
        this.validatorService = validatorService;
        this.htmlBuilder = htmlBuilder;
        this.pdfRenderer = pdfRenderer;
        this.generationMapper = generationMapper;
        this.appProperties = appProperties;
        this.variableRepository = variableRepository;
        this.asyncProcessor = asyncProcessor;
        this.entrepriseService = entrepriseService;
        this.fileStorageService = fileStorageService;
        this.excelRenderer = excelRenderer;
        this.facturXPdfService = facturXPdfService;
        this.reportMetricsService = reportMetricsService;
    }

    // ============================================================
    // Contrôle d'appartenance multi‑entreprise
    // ============================================================

    /**
     * Charge un template et vérifie qu'il appartient à l'entreprise courante.
     * Lève une TemplateNotFoundException si le template n'existe pas ou s'il
     * appartient à une autre entreprise (même comportement que pour un template
     * inexistant).
     */
    private ReportTemplate loadTemplateForCurrentEntreprise(UUID id) {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + id));
        String currentCode = entrepriseService.getCurrentCodeEntreprise();
        
        // Si le template est public (codeEntreprise null), accessible à tous
        if (template.getCodeEntreprise() == null || template.getCodeEntreprise().isBlank()) {
            return template;
        }
        // Si le template est privé, le header doit correspondre
        if (currentCode == null || !currentCode.equals(template.getCodeEntreprise())) {
            throw new TemplateNotFoundException("Template introuvable : " + id);
        }
        return template;
    }

    // ============================================================
    // Méthodes métier
    // ============================================================

    // ---------- SYNCHRONE ----------
    @Transactional
    public byte[] generateSync(UUID templateId, Object rawData) {
        ReportTemplate template = getPublishedTemplate(templateId);
        Map<String, Object> data = toDataMap(rawData);
        ReportGeneration generation = generateAndStoreReport(template, data);
        return fileStorageService.loadFile(generation.getUrlFichierGenere());
    }

    @Transactional
    public ReportGeneration generateAndStoreReport(ReportTemplate template, Map<String, Object> data) {
        // ✅ Validation avec le schéma extrait
        validatorService.validate(template.getSchema(), data);

        long startMs = System.currentTimeMillis();
        ReportGeneration generation = createGenerationEntry(template, data);
        boolean isFacturX = Boolean.TRUE.equals(data.get("factur_x"))
                || Boolean.TRUE.equals(data.get("facturX"))
                || "factur-x".equalsIgnoreCase(String.valueOf(data.get("format")));

        try {
            byte[] pdf = renderPdf(template, data);

            if (isFacturX) {
                String profileStr = String.valueOf(data.getOrDefault("factur_x_profile", "BASIC"));
                com.rapports.moteur.service.facturx.FacturXProfile profile =
                        com.rapports.moteur.service.facturx.FacturXProfile.fromString(profileStr);
                pdf = facturXPdfService.convertToFacturX(pdf, data, profile);
            }

            String storageKey = buildStorageKey(template, generation.getId());
            String savedPath = fileStorageService.storeFile(storageKey, pdf, "application/pdf");
            generation.setUrlFichierGenere(savedPath);
            generation.setStatut(GenerationStatus.SUCCES);
            ReportGeneration saved = generationRepository.save(generation);

            java.time.Duration duration = java.time.Duration.ofMillis(System.currentTimeMillis() - startMs);
            reportMetricsService.recordGeneration(
                    pdfRenderer.getPreferredEngineName(),
                    isFacturX ? "factur-x" : "pdf",
                    "success",
                    template.getCodeEntreprise(),
                    duration
            );

            return saved;
        } catch (Exception e) {
            generation.setStatut(GenerationStatus.ECHEC);
            generationRepository.save(generation);

            java.time.Duration duration = java.time.Duration.ofMillis(System.currentTimeMillis() - startMs);
            reportMetricsService.recordGeneration(
                    pdfRenderer.getPreferredEngineName(),
                    isFacturX ? "factur-x" : "pdf",
                    "error",
                    template.getCodeEntreprise(),
                    duration
            );

            throw new IllegalStateException("Echec de la generation : " + e.getMessage(), e);
        }
    }

    // ---------- ASYNCHRONE ----------
    public GenerationResponse generateAsync(UUID templateId, Object rawData) {
        ReportTemplate template = getPublishedTemplate(templateId);
        Map<String, Object> data = toDataMap(rawData);

        // Validation temporairement désactivée
        // validatorService.validate(variables, data);

        ReportGeneration generation = createGenerationEntry(template, data);
        asyncProcessor.processAsync(generation.getId(), templateId, data,
                appProperties.getStoragePath());

        return GenerationResponse.builder()
                .generationId(generation.getId())
                .status(GenerationStatus.EN_COURS.toString())
                .build();
    }

    // ---------- CONSULTATION ----------
    public GenerationDto getGeneration(UUID generationId) {
        ReportGeneration generation = generationRepository.findById(generationId)
                .orElseThrow(() -> new IllegalStateException("Generation introuvable : " + generationId));

        // Vérifie que le template de la génération appartient à l'entreprise courante
        loadTemplateForCurrentEntreprise(generation.getTemplate().getId());

        return generationMapper.toDto(generation);
    }

    public List<GenerationDto> getHistory(UUID templateId) {
        // Vérifie l'accès au template avant de retourner l'historique
        loadTemplateForCurrentEntreprise(templateId);

        return generationRepository.findByTemplate_IdOrderByDateGenerationDesc(templateId)
                .stream().map(generationMapper::toDto).toList();
    }

    public byte[] downloadPdf(UUID generationId) {
        ReportGeneration generation = generationRepository.findById(generationId)
                .orElseThrow(() -> new IllegalStateException("Generation introuvable : " + generationId));

        // Vérifie l'appartenance
        loadTemplateForCurrentEntreprise(generation.getTemplate().getId());

        if (generation.getUrlFichierGenere() == null) {
            throw new IllegalStateException("Aucun fichier disponible pour cette generation");
        }
        return fileStorageService.loadFile(generation.getUrlFichierGenere());
    }

    public String generateHtml(UUID templateId, Object rawData) {
        ReportTemplate template = loadTemplateForCurrentEntreprise(templateId);
        Map<String, Object> data = toDataMap(rawData);
        validatorService.validate(template.getSchema(), data);
        return htmlBuilder.build(template, data);
    }

    public byte[] generateExcel(UUID templateId, Object rawData) {
        ReportTemplate template = getPublishedTemplate(templateId);
        Map<String, Object> data = toDataMap(rawData);
        validatorService.validate(template.getSchema(), data);
        return excelRenderer.renderToExcel(template, data);
    }

    // ---------- HELPERS ----------

    private ReportTemplate getPublishedTemplate(UUID templateId) {
        // Utilise la méthode sécurisée au lieu d'un simple findById
        ReportTemplate template = loadTemplateForCurrentEntreprise(templateId);
        if (template.getStatut() != TemplateStatus.PUBLIE) {
            throw new ValidationException(List.of("Le template doit etre publie avant generation"));
        }
        return template;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toDataMap(Object rawData) {
        if (rawData instanceof Map) return (Map<String, Object>) rawData;
        throw new ValidationException(List.of("Le corps de la requete doit etre un objet JSON"));
    }

    private ReportGeneration createGenerationEntry(ReportTemplate template, Map<String, Object> data) {
        String donneesJson;
        try {
            donneesJson = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            donneesJson = "{}";
        }
        ReportGeneration generation = ReportGeneration.builder()
                .template(template)
                .donneesRecues(donneesJson)
                .statut(GenerationStatus.EN_COURS)
                .build();
        return generationRepository.save(generation);
    }

    private byte[] renderPdf(ReportTemplate template, Map<String, Object> data) {
        RenderOptions options = RenderOptions.fromTemplate(template);
        return pdfRenderer.renderToPdf(htmlBuilder.build(template, data), options);
    }

    private String buildStorageKey(ReportTemplate template, UUID generationId) {
        String codeEntreprise = template.getCodeEntreprise();
        if (codeEntreprise != null && !codeEntreprise.isBlank()) {
            return "entreprises/" + codeEntreprise.trim() + "/reports/" + generationId + ".pdf";
        }
        return "public/reports/" + generationId + ".pdf";
    }

    private double toNumber(Object value) {
        if (value == null) return 0;
        if (value instanceof Number number) return number.doubleValue();
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        if (value instanceof Boolean bool) return bool ? 1 : 0;
        return 0;
    }
}