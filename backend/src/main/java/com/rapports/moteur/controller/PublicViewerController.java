package com.rapports.moteur.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.ApiResponse;
import com.rapports.moteur.entity.Document;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.exceptions.ResourceNotFoundException;
import com.rapports.moteur.repository.DocumentRepository;
import com.rapports.moteur.service.CodeGeneratorService;
import com.rapports.moteur.service.PdfRendererService;
import com.rapports.moteur.service.TemplateHtmlBuilder;
import com.rapports.moteur.service.rendering.RenderOptions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/public/documents")
@RequiredArgsConstructor
@Tag(name = "Visionneuse Publique", description = "Accès public sécurisé aux documents partagés via lien web et QR Code")
public class PublicViewerController {

    private final DocumentRepository documentRepository;
    private final TemplateHtmlBuilder htmlBuilder;
    private final PdfRendererService pdfRendererService;
    private final CodeGeneratorService codeGeneratorService;
    private final ObjectMapper objectMapper;

    @GetMapping("/view/{shareToken}")
    @Operation(summary = "Consulter un document public via son jeton de partage",
               description = "Permet à un recruteur ou un client de visualiser un document sans compte.")
    public ResponseEntity<ApiResponse<PublicDocumentViewDto>> viewPublicDocument(
            @PathVariable String shareToken,
            @RequestParam(required = false, defaultValue = "http://localhost:4200") String baseUrl) {

        Document doc = documentRepository.findByShareTokenAndIsPublicSharedTrue(shareToken)
                .orElseThrow(() -> new ResourceNotFoundException("Document partagé introuvable ou lien expiré"));

        ReportTemplate template = doc.getTemplate();
        Map<String, Object> data = extractDocumentData(doc.getDonnees());

        String renderedHtml = htmlBuilder.build(template, data);

        String shareUrl = baseUrl + "/view/" + shareToken;
        String qrCodeDataUri = codeGeneratorService.generateQrCodeDataUri(shareUrl, 200);

        PublicDocumentViewDto dto = PublicDocumentViewDto.builder()
                .documentId(doc.getId())
                .nom(doc.getNom())
                .templateNom(template.getNom())
                .formatPapier(template.getFormatPapier())
                .dateCreation(doc.getDateCreation())
                .shareToken(shareToken)
                .shareUrl(shareUrl)
                .qrCodeDataUri(qrCodeDataUri)
                .htmlContent(renderedHtml)
                .build();

        return ResponseEntity.ok(ApiResponse.success("Document public récupéré", dto));
    }

    @GetMapping("/view/{shareToken}/pdf")
    @Operation(summary = "Télécharger le PDF d'un document partagé publiquement",
               description = "Génération du PDF officiel en haute définition sans filigrane pour les documents partagés.")
    public ResponseEntity<byte[]> downloadPublicPdf(@PathVariable String shareToken) {
        Document doc = documentRepository.findByShareTokenAndIsPublicSharedTrue(shareToken)
                .orElseThrow(() -> new ResourceNotFoundException("Document partagé introuvable ou lien expiré"));

        ReportTemplate template = doc.getTemplate();
        Map<String, Object> data = extractDocumentData(doc.getDonnees());

        String renderedHtml = htmlBuilder.build(template, data);
        RenderOptions options = RenderOptions.fromTemplate(template);

        byte[] pdfBytes = pdfRendererService.renderToPdf(renderedHtml, options);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getNom() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private Map<String, Object> extractDocumentData(String donneesJson) {
        if (donneesJson == null || donneesJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(donneesJson, Map.class);
        } catch (Exception e) {
            log.warn("Erreur lecture données document : {}", e.getMessage());
            return new HashMap<>();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublicDocumentViewDto {
        private UUID documentId;
        private String nom;
        private String templateNom;
        private String formatPapier;
        private LocalDateTime dateCreation;
        private String shareToken;
        private String shareUrl;
        private String qrCodeDataUri;
        private String htmlContent;
    }
}
