package com.rapports.moteur.service.facturx;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FacturXPdfServiceTest {

    private FacturXPdfService facturXService;
    private byte[] samplePdfBytes;

    @BeforeEach
    void setUp() throws IOException {
        FacturXXmlGenerator xmlGenerator = new FacturXXmlGenerator();
        facturXService = new FacturXPdfService(xmlGenerator);

        // Crée un PDF minimal valide avec 1 page blanche pour le test
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            doc.save(baos);
            samplePdfBytes = baos.toByteArray();
        }
    }

    @Test
    @DisplayName("Génération Factur-X BASIC : inclusion de factur-x.xml et métadonnées PDF/A-3")
    void testConvertToFacturXBasic() throws IOException {
        FacturXInvoiceData invoice = FacturXInvoiceData.builder()
                .profile(FacturXProfile.BASIC)
                .invoiceNumber("FA-2026-0042")
                .issueDate(LocalDate.of(2026, 3, 15))
                .currency("EUR")
                .sellerName("Acme Corp France")
                .sellerSiret("12345678900012")
                .sellerCity("Paris")
                .sellerPostalCode("75001")
                .buyerName("Client Entreprise SAS")
                .buyerSiret("98765432100098")
                .totalHt(BigDecimal.valueOf(1000.00))
                .totalTva(BigDecimal.valueOf(200.00))
                .totalTtc(BigDecimal.valueOf(1200.00))
                .items(List.of(
                        FacturXInvoiceData.FacturXItemData.builder()
                                .lineId("1")
                                .description("Prestation de conseil technique")
                                .quantity(BigDecimal.valueOf(2))
                                .unitPrice(BigDecimal.valueOf(500.00))
                                .totalHt(BigDecimal.valueOf(1000.00))
                                .vatRate(BigDecimal.valueOf(20.0))
                                .build()
                ))
                .build();

        byte[] facturXPdf = facturXService.convertToFacturX(samplePdfBytes, invoice);

        assertThat(facturXPdf).isNotNull();
        assertThat(facturXPdf.length).isGreaterThan(samplePdfBytes.length);

        // Validation en rechargeant le PDF produit
        try (PDDocument doc = Loader.loadPDF(facturXPdf)) {
            // 1. Vérification du fichier attaché factur-x.xml
            Map<String, PDComplexFileSpecification> embeddedFiles =
                    doc.getDocumentCatalog().getNames().getEmbeddedFiles().getNames();

            assertThat(embeddedFiles).containsKey("factur-x.xml");
            PDComplexFileSpecification fileSpec = embeddedFiles.get("factur-x.xml");
            assertThat(fileSpec.getFilename()).isEqualTo("factur-x.xml");

            byte[] xmlBytes = fileSpec.getEmbeddedFile().toByteArray();
            String xml = new String(xmlBytes, StandardCharsets.UTF_8);

            assertThat(xml).contains("rsm:CrossIndustryInvoice");
            assertThat(xml).contains("urn:factur-x.eu:1p0:basic");
            assertThat(xml).contains("FA-2026-0042");
            assertThat(xml).contains("Acme Corp France");
            assertThat(xml).contains("Client Entreprise SAS");
            assertThat(xml).contains("1000.00");
            assertThat(xml).contains("1200.00");
            assertThat(xml).contains("Prestation de conseil technique");

            // 2. Vérification de l'attribut AFRelationship (Alternative)
            COSBase afRel = fileSpec.getCOSObject().getItem(COSName.getPDFName("AFRelationship"));
            assertThat(afRel).isNotNull();
            assertThat(afRel.toString()).contains("Alternative");

            // 3. Vérification de la présence du tableau /AF au catalogue
            COSArray afArray = (COSArray) doc.getDocumentCatalog().getCOSObject().getItem(COSName.getPDFName("AF"));
            assertThat(afArray).isNotNull();
            assertThat(afArray.size()).isEqualTo(1);

            // 4. Vérification des métadonnées XMP
            String xmp = new String(doc.getDocumentCatalog().getMetadata().toByteArray(), StandardCharsets.UTF_8);
            assertThat(xmp).contains("<pdfaid:part>3</pdfaid:part>");
            assertThat(xmp).contains("<pdfaid:conformance>B</pdfaid:conformance>");
            assertThat(xmp).contains("<fx:ConformanceLevel>BASIC</fx:ConformanceLevel>");
        }
    }

    @Test
    @DisplayName("Génération Factur-X depuis Map libre avec profil MINIMUM")
    void testConvertToFacturXFromMapMinimum() throws IOException {
        Map<String, Object> data = Map.of(
                "numero_facture", "FACT-2026-999",
                "client_nom", "Mairie de Bordeaux",
                "total_ht", 500.50,
                "total_tva", 100.10,
                "total_ttc", 600.60
        );

        byte[] facturXPdf = facturXService.convertToFacturX(samplePdfBytes, data, FacturXProfile.MINIMUM);

        try (PDDocument doc = Loader.loadPDF(facturXPdf)) {
            PDComplexFileSpecification fileSpec = doc.getDocumentCatalog().getNames().getEmbeddedFiles().getNames().get("factur-x.xml");
            String xml = new String(fileSpec.getEmbeddedFile().toByteArray(), StandardCharsets.UTF_8);

            assertThat(xml).contains("urn:factur-x.eu:1p0:minimum");
            assertThat(xml).contains("FACT-2026-999");
            assertThat(xml).contains("Mairie de Bordeaux");
            assertThat(xml).contains("500.50");
            assertThat(xml).contains("600.60");
            // Le profil MINIMUM ne contient pas les lignes de détail d'articles
            assertThat(xml).doesNotContain("IncludedSupplyChainTradeLineItem");

            String xmp = new String(doc.getDocumentCatalog().getMetadata().toByteArray(), StandardCharsets.UTF_8);
            assertThat(xmp).contains("<fx:ConformanceLevel>MINIMUM</fx:ConformanceLevel>");
        }
    }
}

