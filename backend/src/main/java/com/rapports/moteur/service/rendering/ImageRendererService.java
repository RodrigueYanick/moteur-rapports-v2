package com.rapports.moteur.service.rendering;

import com.rapports.moteur.exceptions.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Service de rendu haute définition (300 DPI) transformant un document PDF
 * en image PNG, JPEG ou en archive ZIP multi-pages.
 */
@Slf4j
@Service
public class ImageRendererService {

    public static final int DEFAULT_DPI = 300;
    public static final int MIN_DPI = 72;
    public static final int MAX_DPI = 600;
    public static final float DEFAULT_JPEG_QUALITY = 0.95f;
    public static final int MAX_PAGE_LIMIT = 50;

    /**
     * Rendu complet du PDF sous forme d'image HD ou d'archive ZIP multi-pages.
     *
     * @param pdfBytes    Contenu binaire du PDF d'origine
     * @param format      Format souhaité ("PNG", "JPEG", "JPG")
     * @param targetPage  Numéro de page cible (1-indexed), ou null pour toutes les pages
     * @param dpi         Résolution en points par pouce (ex: 300 pour HD)
     * @param quality     Qualité de compression pour JPEG (0.1 à 1.0)
     * @return Objet ImageExportResult avec données, contentType et métadonnées
     */
    public ImageExportResult renderToImage(byte[] pdfBytes, String format, Integer targetPage, Integer dpi, Float quality) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new ValidationException(List.of("Le flux PDF fourni pour le rendu image est vide"));
        }

        String normalizedFormat = normalizeFormat(format);
        int effectiveDpi = validateAndGetDpi(dpi);
        float effectiveQuality = validateAndGetQuality(quality);

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            int pageCount = document.getNumberOfPages();
            if (pageCount == 0) {
                throw new ValidationException(List.of("Le document PDF ne contient aucune page"));
            }
            if (pageCount > MAX_PAGE_LIMIT) {
                throw new ValidationException(List.of(
                        String.format("Le document dépasse la limite autorisée pour l'export image (%d pages, max: %d)",
                                pageCount, MAX_PAGE_LIMIT)));
            }

            PDFRenderer pdfRenderer = new PDFRenderer(document);

            // Cas 1 : Une page spécifique est demandée
            if (targetPage != null) {
                if (targetPage < 1 || targetPage > pageCount) {
                    throw new ValidationException(List.of(
                            String.format("Page demandée invalide (%d). Le document comporte %d page(s)", targetPage, pageCount)));
                }
                int pageIdx = targetPage - 1;
                byte[] imageBytes = renderSinglePage(pdfRenderer, pageIdx, effectiveDpi, normalizedFormat, effectiveQuality);
                String ext = getExtension(normalizedFormat);
                return ImageExportResult.builder()
                        .data(imageBytes)
                        .contentType(getContentType(normalizedFormat))
                        .filename(String.format("rapport_page_%d%s", targetPage, ext))
                        .pageCount(1)
                        .isZip(false)
                        .build();
            }

            // Cas 2 : Aucune page spécifique et le document n'a qu'une seule page
            if (pageCount == 1) {
                byte[] imageBytes = renderSinglePage(pdfRenderer, 0, effectiveDpi, normalizedFormat, effectiveQuality);
                String ext = getExtension(normalizedFormat);
                return ImageExportResult.builder()
                        .data(imageBytes)
                        .contentType(getContentType(normalizedFormat))
                        .filename("rapport" + ext)
                        .pageCount(1)
                        .isZip(false)
                        .build();
            }

            // Cas 3 : Document multi-pages -> Génération d'une archive ZIP contenant chaque page
            log.info("Rendu multi-pages ({}) en archive ZIP ({}) à {} DPI", pageCount, normalizedFormat, effectiveDpi);
            byte[] zipBytes = renderMultiPagesToZip(pdfRenderer, pageCount, effectiveDpi, normalizedFormat, effectiveQuality);
            return ImageExportResult.builder()
                    .data(zipBytes)
                    .contentType("application/zip")
                    .filename("rapport_pages.zip")
                    .pageCount(pageCount)
                    .isZip(true)
                    .build();

        } catch (IOException e) {
            log.error("Erreur lors de la conversion PDF vers image", e);
            throw new IllegalStateException("Échec de la conversion du document en image : " + e.getMessage(), e);
        }
    }

    private byte[] renderSinglePage(PDFRenderer pdfRenderer, int pageIndex, int dpi, String format, float quality) throws IOException {
        BufferedImage bim = pdfRenderer.renderImageWithDPI(pageIndex, dpi, ImageType.RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        if ("JPEG".equals(format)) {
            writeJpegWithQuality(bim, baos, quality);
        } else {
            ImageIO.write(bim, "PNG", baos);
        }
        return baos.toByteArray();
    }

    private byte[] renderMultiPagesToZip(PDFRenderer pdfRenderer, int pageCount, int dpi, String format, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            String ext = getExtension(format);
            for (int i = 0; i < pageCount; i++) {
                byte[] pageBytes = renderSinglePage(pdfRenderer, i, dpi, format, quality);
                String entryName = String.format("page_%d%s", (i + 1), ext);
                ZipEntry entry = new ZipEntry(entryName);
                entry.setSize(pageBytes.length);
                zos.putNextEntry(entry);
                zos.write(pageBytes);
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    private void writeJpegWithQuality(BufferedImage image, ByteArrayOutputStream baos, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            // Fallback si pas de writer JPEG standard
            ImageIO.write(image, "JPEG", baos);
            return;
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private String normalizeFormat(String format) {
        if (format == null || format.isBlank()) {
            return "PNG";
        }
        String clean = format.trim().toUpperCase();
        if ("JPG".equals(clean) || "JPEG".equals(clean)) {
            return "JPEG";
        }
        if ("PNG".equals(clean)) {
            return "PNG";
        }
        throw new ValidationException(List.of("Format d'image non supporté : " + format + ". Formats valides : PNG, JPEG"));
    }

    private int validateAndGetDpi(Integer dpi) {
        if (dpi == null) {
            return DEFAULT_DPI;
        }
        if (dpi < MIN_DPI || dpi > MAX_DPI) {
            throw new ValidationException(List.of(
                    String.format("La résolution DPI doit être comprise entre %d et %d (reçu: %d)", MIN_DPI, MAX_DPI, dpi)));
        }
        return dpi;
    }

    private float validateAndGetQuality(Float quality) {
        if (quality == null) {
            return DEFAULT_JPEG_QUALITY;
        }
        if (quality < 0.1f || quality > 1.0f) {
            throw new ValidationException(List.of(
                    String.format("La qualité JPEG doit être comprise entre 0.1 et 1.0 (reçu: %.2f)", quality)));
        }
        return quality;
    }

    private String getExtension(String format) {
        return "JPEG".equals(format) ? ".jpg" : ".png";
    }

    private String getContentType(String format) {
        return "JPEG".equals(format) ? "image/jpeg" : "image/png";
    }
}
