package com.rapports.moteur.service.pdf;

import com.rapports.moteur.dto.dtoPdf.PdfSignatureOptions;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfSignerServiceTest {

    private PdfSignerService signerService;
    private byte[] samplePdfBytes;
    private byte[] samplePkcs12Bytes;
    private static final String P12_PASSWORD = "Password123!";

    @BeforeEach
    void setUp() throws Exception {
        signerService = new PdfSignerService();

        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            samplePdfBytes = baos.toByteArray();
        }

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        X500Name subject = new X500Name("CN=RY TECH CACHET, O=RY TECH SOLUTIONS, C=FR");
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date(System.currentTimeMillis() - 1000L * 60);
        Date notAfter = new Date(System.currentTimeMillis() + 1000L * 3600 * 24 * 365);

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject, serial, notBefore, notAfter, subject, kp.getPublic()
        );
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(kp.getPrivate());
        X509CertificateHolder holder = certBuilder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().getCertificate(holder);

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("cachet-entreprise", kp.getPrivate(), P12_PASSWORD.toCharArray(), new Certificate[]{cert});

        ByteArrayOutputStream p12Out = new ByteArrayOutputStream();
        ks.store(p12Out, P12_PASSWORD.toCharArray());
        samplePkcs12Bytes = p12Out.toByteArray();
    }

    @Test
    @DisplayName("Signature numÃ©rique PAdES rÃ©ussie avec mÃ©tadonnÃ©es")
    void shouldSignPdfSuccessfully() throws Exception {
        PdfSignatureOptions options = PdfSignatureOptions.builder()
                .nomSignataire("RY TECH SOLUTIONS")
                .raison("Certification de conformitÃ© lÃ©gale")
                .lieu("Paris")
                .contact("security@ry-tech.com")
                .build();

        byte[] signedPdf = signerService.signPdf(samplePdfBytes, samplePkcs12Bytes, P12_PASSWORD, options);

        assertThat(signedPdf).isNotNull();
        assertThat(signedPdf.length).isGreaterThan(samplePdfBytes.length);

        try (PDDocument doc = Loader.loadPDF(signedPdf)) {
            List<PDSignature> signatures = doc.getSignatureDictionaries();
            assertThat(signatures).hasSize(1);

            PDSignature sig = signatures.get(0);
            assertThat(sig.getName()).isEqualTo("RY TECH SOLUTIONS");
            assertThat(sig.getReason()).isEqualTo("Certification de conformitÃ© lÃ©gale");
            assertThat(sig.getLocation()).isEqualTo("Paris");
            assertThat(sig.getFilter()).isEqualTo(PDSignature.FILTER_ADOBE_PPKLITE.getName());
            assertThat(sig.getSubFilter()).isEqualTo(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED.getName());
        }
    }
}