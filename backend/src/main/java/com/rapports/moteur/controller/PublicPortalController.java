package com.rapports.moteur.controller;

import com.rapports.moteur.dto.dtoTemplate.TemplateResponse;
import com.rapports.moteur.entity.Categorie;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.mapper.TemplateMapper;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.service.PdfRendererService;
import com.rapports.moteur.service.ReportTemplateService;
import com.rapports.moteur.service.rendering.RenderOptions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicPortalController {

    private final ReportTemplateRepository templateRepository;
    private final TemplateMapper templateMapper;
    private final ReportTemplateService templateService;
    private final PdfRendererService pdfRendererService;
    private final com.rapports.moteur.service.TemplateHtmlBuilder htmlBuilder;

    @GetMapping("/templates")
    public ResponseEntity<List<TemplateResponse>> getPublicTemplates(
            @RequestParam(required = false) Categorie categorie,
            @RequestParam(required = false) String q) {

        List<ReportTemplate> templates;
        if (q != null && !q.isBlank()) {
            templates = templateRepository.findByCodeEntrepriseIsNullAndNomContaining(q.trim());
        } else {
            templates = templateRepository.findByCodeEntrepriseIsNull();
        }

        if (categorie != null) {
            templates = templates.stream()
                    .filter(t -> t.getCategorie() == categorie)
                    .collect(Collectors.toList());
        }

        List<TemplateResponse> responses = templates.stream()
                .map(templateMapper::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/templates/{id}/demo-render")
    public ResponseEntity<byte[]> demoRender(
            @PathVariable UUID id,
            @RequestBody(required = false) java.util.Map<String, Object> data) {

        ReportTemplate template = templateRepository.findById(id)
                .filter(t -> t.getCodeEntreprise() == null)
                .orElseThrow(() -> new com.rapports.moteur.exceptions.TemplateNotFoundException("Modèle public introuvable : " + id));

        java.util.Map<String, Object> renderData = (data != null && !data.isEmpty())
                ? data
                : extractSampleData(template.getSchema());

        String html = htmlBuilder.build(template, renderData);

        String watermarkStyle = """
                <div style="position: fixed; top: 45%; left: 50%; transform: translate(-50%, -50%) rotate(-35deg);
                            font-size: 54px; font-weight: 800; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;
                            color: rgba(180, 180, 180, 0.22); border: 4px dashed rgba(180, 180, 180, 0.25);
                            padding: 20px 40px; border-radius: 12px; pointer-events: none; z-index: 99999;
                            text-transform: uppercase; letter-spacing: 5px; white-space: nowrap;">
                    Aperçu Démo Public • Moteur Rapports
                </div>
                """;

        String finalHtml = html;
        if (finalHtml.contains("</body>")) {
            finalHtml = finalHtml.replace("</body>", watermarkStyle + "</body>");
        } else {
            finalHtml = finalHtml + watermarkStyle;
        }

        RenderOptions options = RenderOptions.fromTemplate(template);

        byte[] pdf = pdfRendererService.renderToPdf(finalHtml, options);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"demo-" + template.getNom() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private java.util.Map<String, Object> extractSampleData(String schemaJson) {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        if (schemaJson == null || schemaJson.isBlank()) {
            return data;
        }
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(schemaJson);
            if (root.has("properties")) {
                com.fasterxml.jackson.databind.JsonNode props = root.get("properties");
                props.fieldNames().forEachRemaining(field -> {
                    com.fasterxml.jackson.databind.JsonNode fieldNode = props.get(field);
                    if (fieldNode.has("default")) {
                        data.put(field, fieldNode.get("default").asText());
                    } else if (fieldNode.has("example")) {
                        data.put(field, fieldNode.get("example").asText());
                    }
                });
            } else if (root.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode item : root) {
                    String name = item.path("nomVariable").asText();
                    if (!name.isBlank()) {
                        String def = item.path("valeurParDefaut").asText(item.path("exemple").asText(""));
                        data.put(name, def);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Impossible d'extraire les données d'exemple du schéma : {}", e.getMessage());
        }
        return data;
    }

    @GetMapping("/templates/{id}")
    public ResponseEntity<TemplateResponse> getPublicTemplateById(@PathVariable UUID id) {
        return templateRepository.findById(id)
                .filter(t -> t.getCodeEntreprise() == null)
                .map(templateMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryInfoDto>> getPublicCategories() {
        List<CategoryInfoDto> list = List.of(
                CategoryInfoDto.builder().code(Categorie.CV_CANADIAN.name()).label("CV Canadien").persona("CANDIDATE").description("Format officiel sans photo, conforme aux lois anti-discrimination").icon("FileUser").build(),
                CategoryInfoDto.builder().code(Categorie.CV_ATS.name()).label("CV Américain (ATS)").persona("CANDIDATE").description("1 page stricte, 100% optimisé pour parseurs automatiques").icon("FileText").build(),
                CategoryInfoDto.builder().code(Categorie.CV_EUROPEEN.name()).label("CV Européen").persona("CANDIDATE").description("Design moderne avec photo détourée et barre de compétences").icon("Award").build(),
                CategoryInfoDto.builder().code(Categorie.STAGE_ACADEMIQUE.name()).label("Rapport Académique / Stage").persona("STUDENT").description("Couverture universitaire, sommaire hiérarchique, pagination Page X/Y").icon("GraduationCap").build(),
                CategoryInfoDto.builder().code(Categorie.FACTURE_DEVIS.name()).label("Facture & Devis Freelance").persona("FREELANCE").description("Conformité légale, TVA ventilée, totaux automatiques, IBAN").icon("Receipt").build(),
                CategoryInfoDto.builder().code(Categorie.MARKETING_FLYER.name()).label("Flyer & Événement").persona("INDIVIDUAL").description("Format A5 percutant avec QR Code dynamique").icon("Megaphone").build()
        );
        return ResponseEntity.ok(list);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryInfoDto {
        private String code;
        private String label;
        private String persona;
        private String description;
        private String icon;
    }
}
