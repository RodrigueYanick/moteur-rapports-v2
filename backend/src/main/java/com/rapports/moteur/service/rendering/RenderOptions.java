package com.rapports.moteur.service.rendering;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Options et métadonnées de rendu transmises aux moteurs de génération PDF.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenderOptions {

    @Builder.Default
    private String formatPapier = "A4";

    private Integer largeurMm;
    private Integer hauteurMm;

    private Integer margeHautMm;
    private Integer margeBasMm;
    private Integer margeGaucheMm;
    private Integer margeDroiteMm;

    @Builder.Default
    private boolean printBackground = true;

    @Builder.Default
    private boolean preferCssPageSize = true;

    @Builder.Default
    private String emulatedMediaType = "print";

    public static RenderOptions defaultA4() {
        return RenderOptions.builder()
                .formatPapier("A4")
                .largeurMm(210)
                .hauteurMm(297)
                .margeHautMm(10)
                .margeBasMm(10)
                .margeGaucheMm(10)
                .margeDroiteMm(10)
                .printBackground(true)
                .preferCssPageSize(true)
                .emulatedMediaType("print")
                .build();
    }

    public static RenderOptions fromTemplate(com.rapports.moteur.entity.ReportTemplate template) {
        if (template == null) {
            return defaultA4();
        }
        return RenderOptions.builder()
                .formatPapier(template.getFormatPapier() != null ? template.getFormatPapier() : "A4")
                .largeurMm(template.getLargeurMm())
                .hauteurMm(template.getHauteurMm())
                .margeHautMm(template.getMargeHautMm())
                .margeBasMm(template.getMargeBasMm())
                .margeGaucheMm(template.getMargeGaucheMm())
                .margeDroiteMm(template.getMargeDroiteMm())
                .printBackground(true)
                .preferCssPageSize(true)
                .emulatedMediaType("print")
                .build();
    }
}
