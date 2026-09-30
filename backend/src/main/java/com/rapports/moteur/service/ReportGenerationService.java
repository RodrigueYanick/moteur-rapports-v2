package com.rapports.moteur.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.config.AppProperties;
import com.rapports.moteur.dto.dtoGeneration.GenerationDto;
import com.rapports.moteur.dto.dtoGeneration.GenerationResponse;
import com.rapports.moteur.dto.dtoPdf.PdfProtectionOptions;
import com.rapports.moteur.dto.dtoPdf.PdfSignatureOptions;
import com.rapports.moteur.service.audit.AuditTrailService;
import com.rapports.moteur.service.pdf.PdfSecurityService;
import com.rapports.moteur.service.pdf.PdfSignerService;

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
    private final com.rapports.moteur.service.datasource.DataSourceExecutionService dataSourceExecutionService;
    private final com.rapports.moteur.service.rendering.ImageRendererService imageRendererService;
    private final RawDataExportService rawDataExportService;
    private final PdfSecurityService pdfSecurityService;
    private final PdfSignerService pdfSignerService;
    private final CompanyCertificateService companyCertificateService;
    private final AuditTrailService auditTrailService;


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
                                    com.rapports.moteur.service.metrics.ReportMetricsService reportMetricsService,
                                    com.rapports.moteur.service.datasource.DataSourceExecutionService dataSourceExecutionService,
                                    com.rapports.moteur.service.rendering.ImageRendererService imageRendererService,
                                    RawDataExportService rawDataExportService,
                                    PdfSecurityService pdfSecurityService,
                                    PdfSignerService pdfSignerService,
                                    CompanyCertificateService companyCertificateService,
                                    AuditTrailService auditTrailService) {
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
        this.dataSourceExecutionService = dataSourceExecutionService;
        this.imageRendererService = imageRendererService;
        this.rawDataExportService = rawDataExportService;
        this.pdfSecurityService = pdfSecurityService;
        this.pdfSignerService = pdfSignerService;
        this.companyCertificateService = companyCertificateService;
        this.auditTrailService = auditTrailService;
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
        Map<String, Object> data = resolveData(template, rawData);
        ReportGeneration generation = generateAndStoreReport(template, data);
        return fileStorageService.loadFile(generation.getUrlFichierGenere());
    }

    @Transactional
    public ReportGeneration generateAndStoreReport(ReportTemplate template, Map<String, Object> data) {
        // ✅ Validation avec le schéma extrait
        validatorService.validate(template.getSchema(), data);

        PdfProtectionOptions protectionOptions = extractProtectionOptions(data);
        PdfSignatureOptions signatureOptions = extractSignatureOptions(data);

        boolean hasProtection = protectionOptions != null && protectionOptions.isProtectionRequise();
        boolean hasSignature = signatureOptions != null && signatureOptions.getCertificateId() != null;

        if (hasProtection && hasSignature) {
            throw new ValidationException(List.of(
                    "Un document PDF ne peut pas être simultanément signé numériquement et chiffré par mot de passe. Veuillez choisir la signature électronique (authenticité) ou le chiffrement par mot de passe (confidentialité)."
            ));
        }

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

            if (hasSignature) {
                CompanyCertificateService.DecryptedCertificate decryptedCert =
                        companyCertificateService.loadDecryptedCertificate(signatureOptions.getCertificateId());
                pdf = pdfSignerService.signPdf(pdf, decryptedCert.pkcs12Bytes(), decryptedCert.password(), signatureOptions);
            } else if (hasProtection) {
                pdf = pdfSecurityService.protectPdf(pdf, protectionOptions);
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

            auditTrailService.logAction(
                    "DOCUMENT_GENERATE",
                    "REPORT_GENERATION",
                    generation.getId().toString(),
                    Map.of(
                            "templateId", template.getId().toString(),
                            "templateNom", template.getNom() != null ? template.getNom() : "",
                            "statut", "SUCCES",
                            "hasSignature", hasSignature,
                            "hasProtection", hasProtection,
                            "isFacturX", isFacturX
                    )
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

            auditTrailService.logAction(
                    "DOCUMENT_GENERATE",
                    "REPORT_GENERATION",
                    generation.getId().toString(),
                    Map.of(
                            "templateId", template.getId().toString(),
                            "templateNom", template.getNom() != null ? template.getNom() : "",
                            "statut", "ECHEC",
                            "error", e.getMessage() != null ? e.getMessage() : "Erreur inconnue"
                    )
            );

            if (e instanceof ValidationException ve) {
                throw ve;
            }
            throw new IllegalStateException("Echec de la generation : " + e.getMessage(), e);
        }
    }

    // ---------- ASYNCHRONE ----------
    public GenerationResponse generateAsync(UUID templateId, Object rawData) {
        ReportTemplate template = getPublishedTemplate(templateId);
        Map<String, Object> data = resolveData(template, rawData);

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
        ReportTemplate template = loadTemplateForCurrentEntreprise(generation.getTemplate().getId());

        if (generation.getUrlFichierGenere() == null) {
            throw new IllegalStateException("Aucun fichier disponible pour cette generation");
        }
        byte[] bytes = fileStorageService.loadFile(generation.getUrlFichierGenere());

        auditTrailService.logAction(
                "DOCUMENT_DOWNLOAD",
                "REPORT_GENERATION",
                generationId.toString(),
                Map.of(
                        "templateId", template.getId().toString(),
                        "templateNom", template.getNom() != null ? template.getNom() : "",
                        "statut", "SUCCES"
                )
        );

        return bytes;
    }

    public String generateHtml(UUID templateId, Object rawData) {
        ReportTemplate template = loadTemplateForCurrentEntreprise(templateId);
        Map<String, Object> data = resolveData(template, rawData);
        validatorService.validate(template.getSchema(), data);
        return htmlBuilder.build(template, data);
    }

    public byte[] generateExcel(UUID templateId, Object rawData) {
        ReportTemplate template = getPublishedTemplate(templateId);
        Map<String, Object> data = resolveData(template, rawData);
        validatorService.validate(template.getSchema(), data);
        return excelRenderer.renderToExcel(template, data);
    }

    public com.rapports.moteur.service.rendering.ImageExportResult generateImage(UUID templateId, Object rawData,
                                                                                 String format, Integer page,
                                                                                 Integer dpi, Float quality) {
        ReportTemplate template = getPublishedTemplate(templateId);
        Map<String, Object> data = resolveData(template, rawData);
        validatorService.validate(template.getSchema(), data);
        byte[] pdfBytes = renderPdf(template, data);
        return imageRendererService.renderToImage(pdfBytes, format, page, dpi, quality);
    }

    public byte[] generateCsv(UUID templateId, Object rawData, Character delimiter) {
        ReportTemplate template = getPublishedTemplate(templateId);
        Map<String, Object> data = resolveData(template, rawData);
        validatorService.validate(template.getSchema(), data);
        return rawDataExportService.exportToCsv(template, data, delimiter);
    }

    public byte[] generateJson(UUID templateId, Object rawData) {
        ReportTemplate template = getPublishedTemplate(templateId);
        Map<String, Object> data = resolveData(template, rawData);
        validatorService.validate(template.getSchema(), data);
        return rawDataExportService.exportToJson(template, data);
    }

    public Map<String, Object> resolveData(ReportTemplate template, Object rawData) {
        Map<String, Object> callerData = toDataMap(rawData);
        if (template.getDataSource() != null && Boolean.TRUE.equals(template.getDataSource().getActif())
                && template.getDataSourceQuery() != null && !template.getDataSourceQuery().isBlank()) {
            try {
                Object fetched = dataSourceExecutionService.execute(
                        template.getDataSource(),
                        template.getDataSourceQuery(),
                        callerData,
                        1000
                );
                if (fetched instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fetchedMap = (Map<String, Object>) fetched;
                    Map<String, Object> merged = new java.util.HashMap<>(fetchedMap);
                    merged.putAll(callerData);
                    return merged;
                } else if (fetched instanceof List) {
                    Map<String, Object> merged = new java.util.HashMap<>(callerData);
                    merged.put("lignes", fetched);
                    merged.put("items", fetched);
                    merged.put("data", fetched);
                    return merged;
                }
            } catch (Exception e) {
                if (callerData.isEmpty()) {
                    throw new ValidationException("Échec de la récupération des données depuis la source distante : " + e.getMessage());
                }
            }
        }
        return callerData;
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
        if (rawData == null) return new java.util.HashMap<>();
        if (rawData instanceof Map) return (Map<String, Object>) rawData;
        if (rawData instanceof String str) {
            try {
                return objectMapper.readValue(str, Map.class);
            } catch (Exception ignored) {}
        }
        if (rawData instanceof com.fasterxml.jackson.databind.node.TextNode textNode) {
            try {
                return objectMapper.readValue(textNode.asText(), Map.class);
            } catch (Exception ignored) {}
        }
        try {
            return objectMapper.convertValue(rawData, Map.class);
        } catch (Exception e) {
            throw new ValidationException(List.of("Le corps de la requete doit etre un objet JSON"));
        }
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

    private PdfProtectionOptions extractProtectionOptions(Map<String, Object> data) {
        if (data == null || data.isEmpty()) return null;

        if (data.containsKey("protection") && data.get("protection") instanceof Map) {
            try {
                return objectMapper.convertValue(data.get("protection"), PdfProtectionOptions.class);
            } catch (Exception ignored) {}
        }

        String userPwd = null;
        if (data.containsKey("mot_de_passe_utilisateur")) userPwd = String.valueOf(data.get("mot_de_passe_utilisateur"));
        else if (data.containsKey("motDePasseUtilisateur")) userPwd = String.valueOf(data.get("motDePasseUtilisateur"));
        else if (data.containsKey("user_password")) userPwd = String.valueOf(data.get("user_password"));

        String ownerPwd = null;
        if (data.containsKey("mot_de_passe_proprietaire")) ownerPwd = String.valueOf(data.get("mot_de_passe_proprietaire"));
        else if (data.containsKey("motDePasseProprietaire")) ownerPwd = String.valueOf(data.get("motDePasseProprietaire"));
        else if (data.containsKey("owner_password")) ownerPwd = String.valueOf(data.get("owner_password"));

        if ((userPwd != null && !userPwd.isBlank()) || (ownerPwd != null && !ownerPwd.isBlank())) {
            boolean canPrint = true;
            if (data.containsKey("autoriser_impression")) canPrint = Boolean.parseBoolean(String.valueOf(data.get("autoriser_impression")));
            else if (data.containsKey("autoriserImpression")) canPrint = Boolean.parseBoolean(String.valueOf(data.get("autoriserImpression")));

            boolean canCopy = false;
            if (data.containsKey("autoriser_copie")) canCopy = Boolean.parseBoolean(String.valueOf(data.get("autoriser_copie")));
            else if (data.containsKey("autoriserCopie")) canCopy = Boolean.parseBoolean(String.valueOf(data.get("autoriserCopie")));

            boolean canModify = false;
            if (data.containsKey("autoriser_modification")) canModify = Boolean.parseBoolean(String.valueOf(data.get("autoriser_modification")));
            else if (data.containsKey("autoriserModification")) canModify = Boolean.parseBoolean(String.valueOf(data.get("autoriserModification")));

            return PdfProtectionOptions.builder()
                    .motDePasseUtilisateur(userPwd)
                    .motDePasseProprietaire(ownerPwd)
                    .autoriserImpression(canPrint)
                    .autoriserCopie(canCopy)
                    .autoriserModification(canModify)
                    .tailleCleBits(256)
                    .build();
        }
        return null;
    }

    private PdfSignatureOptions extractSignatureOptions(Map<String, Object> data) {
        if (data == null || data.isEmpty()) return null;

        if (data.containsKey("signature") && data.get("signature") instanceof Map) {
            try {
                return objectMapper.convertValue(data.get("signature"), PdfSignatureOptions.class);
            } catch (Exception ignored) {}
        }

        Object certIdObj = data.get("signature_certificate_id");
        if (certIdObj == null) certIdObj = data.get("signatureCertificateId");
        if (certIdObj == null) certIdObj = data.get("certificate_id");

        if (certIdObj != null && !String.valueOf(certIdObj).isBlank()) {
            UUID certId;
            try {
                certId = certIdObj instanceof UUID ? (UUID) certIdObj : UUID.fromString(String.valueOf(certIdObj));
            } catch (Exception e) {
                throw new ValidationException(List.of("Identifiant du certificat de signature invalide"));
            }

            String signataire = data.containsKey("nom_signataire") ? String.valueOf(data.get("nom_signataire")) : (data.containsKey("nomSignataire") ? String.valueOf(data.get("nomSignataire")) : null);
            String raison = data.containsKey("raison_signature") ? String.valueOf(data.get("raison_signature")) : (data.containsKey("raisonSignature") ? String.valueOf(data.get("raisonSignature")) : "Certification de conformite");
            String lieu = data.containsKey("lieu_signature") ? String.valueOf(data.get("lieu_signature")) : (data.containsKey("lieuSignature") ? String.valueOf(data.get("lieuSignature")) : null);

            return PdfSignatureOptions.builder()
                    .certificateId(certId)
                    .nomSignataire(signataire)
                    .raison(raison)
                    .lieu(lieu)
                    .signatureVisible(false)
                    .build();
        }
        return null;
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