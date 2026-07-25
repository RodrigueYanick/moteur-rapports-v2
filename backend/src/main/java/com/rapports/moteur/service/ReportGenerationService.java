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

    // ⚠️ variableRepository a été retiré
    public ReportGenerationService(ReportGenerationRepository generationRepository,
                                    ReportTemplateRepository templateRepository,
                                    DataValidatorService validatorService,
                                    TemplateHtmlBuilder htmlBuilder,
                                    PdfRendererService pdfRenderer,
                                    GenerationMapper generationMapper,
                                    AppProperties appProperties,
                                    AsyncGenerationProcessor asyncProcessor) {
        this.generationRepository = generationRepository;
        this.templateRepository = templateRepository;
        this.validatorService = validatorService;
        this.htmlBuilder = htmlBuilder;
        this.pdfRenderer = pdfRenderer;
        this.generationMapper = generationMapper;
        this.appProperties = appProperties;
        this.asyncProcessor = asyncProcessor;
    }

    // ---------- SYNCHRONE ----------
    @Transactional
    public byte[] generateSync(UUID templateId, Object rawData) {
        ReportTemplate template = getPublishedTemplate(templateId);
        Map<String, Object> data = toDataMap(rawData);

        // ✅ Validation avec le schéma extrait
        validatorService.validate(template.getSchema(), data);

        ReportGeneration generation = createGenerationEntry(template, data);
        try {
            byte[] pdf = renderPdf(template, data);
            generation.setUrlFichierGenere(storePdf(generation.getId(), pdf));
            generation.setStatut(GenerationStatus.SUCCES);
            generationRepository.save(generation);
            return pdf;
        } catch (Exception e) {
            generation.setStatut(GenerationStatus.ECHEC);
            generationRepository.save(generation);
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
        return generationMapper.toDto(generationRepository.findById(generationId)
                .orElseThrow(() -> new IllegalStateException("Generation introuvable : " + generationId)));
    }

    public List<GenerationDto> getHistory(UUID templateId) {
        return generationRepository.findByTemplate_IdOrderByDateGenerationDesc(templateId)
                .stream().map(generationMapper::toDto).toList();
    }

    public byte[] downloadPdf(UUID generationId) {
        ReportGeneration generation = generationRepository.findById(generationId)
                .orElseThrow(() -> new IllegalStateException("Generation introuvable : " + generationId));
        if (generation.getUrlFichierGenere() == null) {
            throw new IllegalStateException("Aucun fichier disponible pour cette generation");
        }
        try {
            return Files.readAllBytes(Paths.get(generation.getUrlFichierGenere()));
        } catch (IOException e) {
            throw new IllegalStateException("Fichier introuvable sur le disque", e);
        }
    }

    // ---------- HELPERS ----------
    private ReportTemplate getPublishedTemplate(UUID templateId) {
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + templateId));
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
        return pdfRenderer.renderToPdf(htmlBuilder.build(template.getContenuDesign(), data));
    }

    private String storePdf(UUID generationId, byte[] pdf) {
        try {
            Path dir = Paths.get(appProperties.getStoragePath());
            Files.createDirectories(dir);
            Path file = dir.resolve(generationId + ".pdf");
            Files.write(file, pdf);
            return file.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de stocker le PDF", e);
        }
    }
}