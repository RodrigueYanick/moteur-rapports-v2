package com.rapports.moteur.service.pdf;

import com.rapports.moteur.dto.dtoPdf.PdfSignatureOptions;
import com.rapports.moteur.exceptions.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;

@Slf4j
@Service
public class PdfSignerService {

    public byte[] signPdf(byte[] pdfBytes, byte[] pkcs12Bytes, String password, PdfSignatureOptions options) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new ValidationException(List.of("Le flux PDF fourni pour la signature est vide"));
        }
        if (pkcs12Bytes == null || pkcs12Bytes.length == 0) {
            throw new ValidationException(List.of("Le certificat PKCS#12 fourni est vide"));
        }

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            char[] passwordChars = password != null ? password.toCharArray() : new char[0];
            keyStore.load(new ByteArrayInputStream(pkcs12Bytes), passwordChars);

            String alias = null;
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String a = aliases.nextElement();
                if (keyStore.isKeyEntry(a)) {
                    alias = a;
                    break;
                }
            }

            if (alias == null) {
                throw new ValidationException(List.of("Aucune clé privée trouvée dans le magasin PKCS#12"));
            }

            PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, passwordChars);
            Certificate[] certificateChain = keyStore.getCertificateChain(alias);
            if (certificateChain == null || certificateChain.length == 0) {
                throw new ValidationException(List.of("Aucune chaîne de certification trouvée pour l'alias : " + alias));
            }

            return signPdfWithPrivateKey(pdfBytes, privateKey, certificateChain, options);
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            throw new ValidationException(List.of("Échec de la signature numérique du PDF : " + e.getMessage()));
        }
    }

    public byte[] signPdfWithPrivateKey(byte[] pdfBytes, PrivateKey privateKey, Certificate[] certificateChain, PdfSignatureOptions options) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);

            String signerName = (options != null && options.getNomSignataire() != null && !options.getNomSignataire().isBlank())
                    ? options.getNomSignataire()
                    : ((X509Certificate) certificateChain[0]).getSubjectX500Principal().getName();
            signature.setName(signerName);

            if (options != null) {
                if (options.getRaison() != null) signature.setReason(options.getRaison());
                if (options.getLieu() != null) signature.setLocation(options.getLieu());
                if (options.getContact() != null) signature.setContactInfo(options.getContact());
            }

            signature.setSignDate(Calendar.getInstance());

            SignatureInterface signatureInterface = content -> {
                try {
                    byte[] contentBytes = content.readAllBytes();
                    List<Certificate> certList = Arrays.asList(certificateChain);
                    JcaCertStore certs = new JcaCertStore(certList);
                    CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
                    ContentSigner sha256Signer = new JcaContentSignerBuilder("SHA256withRSA").build(privateKey);
                    gen.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build()).build(sha256Signer, (X509Certificate) certificateChain[0]));
                    gen.addCertificates(certs);
                    CMSProcessableByteArray msg = new CMSProcessableByteArray(contentBytes);
                    CMSSignedData signedData = gen.generate(msg, false);
                    return signedData.getEncoded();
                } catch (Exception e) {
                    throw new IOException("Erreur CMS: " + e.getMessage(), e);
                }
            };

            SignatureOptions sigOptions = new SignatureOptions();
            sigOptions.setPreferredSignatureSize(SignatureOptions.DEFAULT_SIGNATURE_SIZE * 2);
            document.addSignature(signature, signatureInterface, sigOptions);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.saveIncremental(baos);
            return baos.toByteArray();
        }
    }
}