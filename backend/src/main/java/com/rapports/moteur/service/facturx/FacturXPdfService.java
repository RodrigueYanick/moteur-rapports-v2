package com.rapports.moteur.service.facturx;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacturXPdfService {

    private final FacturXXmlGenerator xmlGenerator;

    /**
     * Convertit un PDF standard en document Factur-X / PDF/A-3 en intégrant le flux XML
     * et les métadonnées XMP requises par la norme européenne EN 16931 / Chorus Pro.
     *
     * @param originalPdfBytes Contenu binaire du PDF d'origine
     * @param invoiceData      Données structurées de facturation
     * @return Contenu binaire du PDF/A-3 enrichi Factur-X
     */
    public byte[] convertToFacturX(byte[] originalPdfBytes, FacturXInvoiceData invoiceData) throws IOException {
        if (originalPdfBytes == null || originalPdfBytes.length == 0) {
            throw new IllegalArgumentException("Le document PDF d'origine ne peut pas être vide");
        }
        if (invoiceData == null) {
            invoiceData = FacturXInvoiceData.builder().build();
        }

        // 1. Génération du flux XML Factur-X
        String xmlContent = xmlGenerator.generateXml(invoiceData);
        byte[] xmlBytes = xmlContent.getBytes(StandardCharsets.UTF_8);

        // 2. Chargement du document PDF via PDFBox 3
        try (PDDocument doc = Loader.loadPDF(originalPdfBytes);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            PDDocumentCatalog catalog = doc.getDocumentCatalog();

            // 3. Création du fichier embarqué factur-x.xml (AFRelationship: Alternative)
            PDEmbeddedFile embeddedFile = new PDEmbeddedFile(doc, new ByteArrayInputStream(xmlBytes));
            embeddedFile.setSubtype("text/xml");
            embeddedFile.setSize(xmlBytes.length);
            Calendar now = Calendar.getInstance();
            embeddedFile.setCreationDate(now);
            embeddedFile.setModDate(now);

            PDComplexFileSpecification fileSpec = new PDComplexFileSpecification();
            fileSpec.setFile("factur-x.xml");
            fileSpec.setFileUnicode("factur-x.xml");
            fileSpec.setEmbeddedFile(embeddedFile);
            fileSpec.getCOSObject().setName(COSName.getPDFName("AFRelationship"), "Alternative");

            // Ajout du fichier au tableau /AF du catalogue (requis PDF/A-3 ISO 19005-3)
            COSArray afArray = new COSArray();
            afArray.add(fileSpec.getCOSObject());
            catalog.getCOSObject().setItem(COSName.getPDFName("AF"), afArray);

            // Ajout du fichier au dictionnaire Names / EmbeddedFiles
            PDDocumentNameDictionary names = catalog.getNames();
            if (names == null) {
                names = new PDDocumentNameDictionary(catalog);
                catalog.setNames(names);
            }
            PDEmbeddedFilesNameTreeNode efTree = new PDEmbeddedFilesNameTreeNode();
            efTree.setNames(Collections.singletonMap("factur-x.xml", fileSpec));
            names.setEmbeddedFiles(efTree);

            // 4. Injection des métadonnées XMP Factur-X (PDF/A-3B)
            injectFacturXXmpMetadata(doc, invoiceData.getProfile().getConformanceLevel());

            // 5. Sauvegarde et retour
            doc.save(baos);
            log.info("PDF converti avec succès au format Factur-X (profil {}, taille XML {} octets)",
                    invoiceData.getProfile().getConformanceLevel(), xmlBytes.length);
            return baos.toByteArray();
        }
    }

    /**
     * Surcharge facilitant l'appel depuis un Map de données de template.
     */
    public byte[] convertToFacturX(byte[] originalPdfBytes, Map<String, Object> templateData, FacturXProfile profile) throws IOException {
        FacturXInvoiceData invoiceData = FacturXInvoiceData.fromMap(templateData, profile);
        return convertToFacturX(originalPdfBytes, invoiceData);
    }

    private void injectFacturXXmpMetadata(PDDocument doc, String conformanceLevel) throws IOException {
        String xmp = """
                <?xpacket begin="﻿" id="W5M0MpCehiHzreSzNTczkc9d"?>
                <x:xmpmeta xmlns:x="adobe:ns:meta/">
                  <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
                    <rdf:Description rdf:about="" xmlns:pdfaid="http://www.aiim.org/pdfa/ns/id/">
                      <pdfaid:part>3</pdfaid:part>
                      <pdfaid:conformance>B</pdfaid:conformance>
                    </rdf:Description>
                    <rdf:Description rdf:about="" xmlns:fx="urn:factur-x:pdfa:CrossIndustryInvoice:1p0#">
                      <fx:DocumentFileName>factur-x.xml</fx:DocumentFileName>
                      <fx:DocumentType>INVOICE</fx:DocumentType>
                      <fx:Version>1.0</fx:Version>
                      <fx:ConformanceLevel>%s</fx:ConformanceLevel>
                    </rdf:Description>
                  </rdf:RDF>
                </x:xmpmeta>
                <?xpacket end="w"?>
                """.formatted(conformanceLevel);

        PDMetadata metadata = new PDMetadata(doc);
        metadata.importXMPMetadata(xmp.getBytes(StandardCharsets.UTF_8));
        doc.getDocumentCatalog().setMetadata(metadata);
    }
}

