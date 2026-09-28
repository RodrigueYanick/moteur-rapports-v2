package com.rapports.moteur.service.rendering;

import com.rapports.moteur.exceptions.ValidationException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageRendererServiceTest {

    private ImageRendererService imageRendererService;

    @BeforeEach
    void setUp() {
        imageRendererService = new ImageRendererService();
    }

    private byte[] createSamplePdf(int pageCount) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            for (int i = 1; i <= pageCount; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18);
                    cs.newLineAtOffset(100, 700);
                    cs.showText("Page de test #" + i);
                    cs.endText();
                }
            }
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    @DisplayName("Devrait rendre un PDF mono-page en image PNG HD (300 DPI)")
    void testRenderSinglePagePng() throws Exception {
        byte[] pdf = createSamplePdf(1);
        ImageExportResult result = imageRendererService.renderToImage(pdf, "PNG", null, 300, null);

        assertThat(result).isNotNull();
        assertThat(result.isZip()).isFalse();
        assertThat(result.getContentType()).isEqualTo("image/png");
        assertThat(result.getFilename()).isEqualTo("rapport.png");
        assertThat(result.getData()).isNotEmpty();

        // Magic bytes PNG : 0x89 'P' 'N' 'G'
        byte[] data = result.getData();
        assertThat(data[0]).isEqualTo((byte) 0x89);
        assertThat(data[1]).isEqualTo((byte) 'P');
        assertThat(data[2]).isEqualTo((byte) 'N');
        assertThat(data[3]).isEqualTo((byte) 'G');
    }

    @Test
    @DisplayName("Devrait rendre un PDF mono-page en JPEG")
    void testRenderSinglePageJpeg() throws Exception {
        byte[] pdf = createSamplePdf(1);
        ImageExportResult result = imageRendererService.renderToImage(pdf, "JPEG", null, 150, 0.90f);

        assertThat(result).isNotNull();
        assertThat(result.isZip()).isFalse();
        assertThat(result.getContentType()).isEqualTo("image/jpeg");
        assertThat(result.getFilename()).isEqualTo("rapport.jpg");
        assertThat(result.getData()).isNotEmpty();

        // Magic bytes JPEG : 0xFF 0xD8 0xFF
        byte[] data = result.getData();
        assertThat(data[0]).isEqualTo((byte) 0xFF);
        assertThat(data[1]).isEqualTo((byte) 0xD8);
        assertThat(data[2]).isEqualTo((byte) 0xFF);
    }

    @Test
    @DisplayName("Devrait rendre une page spécifique d'un PDF multi-pages")
    void testRenderSpecificPage() throws Exception {
        byte[] pdf = createSamplePdf(3);
        ImageExportResult result = imageRendererService.renderToImage(pdf, "PNG", 2, 150, null);

        assertThat(result).isNotNull();
        assertThat(result.isZip()).isFalse();
        assertThat(result.getFilename()).isEqualTo("rapport_page_2.png");
        assertThat(result.getContentType()).isEqualTo("image/png");
        assertThat(result.getPageCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Devrait empaqueter un PDF multi-pages dans un fichier ZIP")
    void testRenderMultiPageToZip() throws Exception {
        byte[] pdf = createSamplePdf(3);
        ImageExportResult result = imageRendererService.renderToImage(pdf, "PNG", null, 100, null);

        assertThat(result).isNotNull();
        assertThat(result.isZip()).isTrue();
        assertThat(result.getContentType()).isEqualTo("application/zip");
        assertThat(result.getFilename()).isEqualTo("rapport_pages.zip");
        assertThat(result.getPageCount()).isEqualTo(3);

        // Vérifier le contenu du ZIP
        int entryCount = 0;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(result.getData()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                assertThat(entry.getName()).matches("page_\\d+\\.png");
            }
        }
        assertThat(entryCount).isEqualTo(3);
    }

    @Test
    @DisplayName("Devrait rejeter un format d'image inconnu")
    void testRejectInvalidFormat() throws Exception {
        byte[] pdf = createSamplePdf(1);
        assertThatThrownBy(() -> imageRendererService.renderToImage(pdf, "WEBP_UNKNOWN", null, 300, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Format d'image non supporté");
    }

    @Test
    @DisplayName("Devrait rejeter un DPI invalide (trop bas ou trop haut)")
    void testRejectInvalidDpi() throws Exception {
        byte[] pdf = createSamplePdf(1);
        assertThatThrownBy(() -> imageRendererService.renderToImage(pdf, "PNG", null, 30, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("DPI");

        assertThatThrownBy(() -> imageRendererService.renderToImage(pdf, "PNG", null, 1200, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("DPI");
    }

    @Test
    @DisplayName("Devrait rejeter un numéro de page hors limites")
    void testRejectPageOutOfBounds() throws Exception {
        byte[] pdf = createSamplePdf(2);
        assertThatThrownBy(() -> imageRendererService.renderToImage(pdf, "PNG", 5, 300, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Page demandée invalide");
    }
}
