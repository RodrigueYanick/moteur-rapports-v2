package com.rapports.moteur.integration;

import com.rapports.moteur.entity.*;
import com.rapports.moteur.security.crypto.AesCryptoService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Tests d'Intégration : Sécurité PDF, Signature Numérique et Audit Trail")
class ReportSecurityAndSignatureIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AesCryptoService aesCryptoService;

    @Autowired
    private com.rapports.moteur.repository.CompanyCertificateRepository companyCertificateRepository;

    @Autowired
    private com.rapports.moteur.repository.AuditLogRepository auditLogRepository;

    private Entreprise testEntreprise;
    private User testUser;
    private String token;
    private ReportTemplate testTemplate;

    private String p12Base64;
    private static final String P12_PASS = "SecretPass123!";

    @BeforeEach
    void setUpTestData() throws Exception {
        testEntreprise = createEntreprise("SECURITY_CORP", "Security Corp");
        testUser = createUser("admin@security.com", "Password123!", Role.ADMIN_ENTREPRISE, testEntreprise);
        token = getBearerToken(testUser);

        testTemplate = createTemplate("Fiche de Paie Confidentielle", "SECURITY_CORP", TemplateStatus.PUBLIE);
        testTemplate.setContenuDesign("{\"pages\":[{\"blocs\":[{\"type\":\"text\",\"contenu\":\"Bulletin pour {{nom_employe}}\"}]}]}");
        templateRepository.save(testTemplate);

        // Création PKCS#12 en mémoire
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        X500Name subject = new X500Name("CN=Security Corp Cachet, O=Security Corp, C=FR");
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
        ks.setKeyEntry("alias-security", kp.getPrivate(), P12_PASS.toCharArray(), new Certificate[]{cert});

        ByteArrayOutputStream p12Out = new ByteArrayOutputStream();
        ks.store(p12Out, P12_PASS.toCharArray());
        p12Base64 = Base64.getEncoder().encodeToString(p12Out.toByteArray());
    }

    @Test
    @DisplayName("Génération d'un PDF protégé par mot de passe utilisateur et propriétaire")
    void shouldGeneratePasswordProtectedPdf() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("nom_employe", "Jean Dupont");
        data.put("mot_de_passe_utilisateur", "UserSecret2026");
        data.put("mot_de_passe_proprietaire", "OwnerSecret2026");
        data.put("autoriser_copie", false);
        data.put("autoriser_impression", true);

        byte[] pdfBytes = mockMvc.perform(post("/api/templates/" + testTemplate.getId() + "/generate")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdfBytes).isNotEmpty();

        // 1. Tenter d'ouvrir sans mot de passe doit échouer
        assertThatThrownBy(() -> Loader.loadPDF(pdfBytes))
                .isInstanceOf(InvalidPasswordException.class);

        // 2. Ouvrir avec le mot de passe utilisateur
        try (PDDocument doc = Loader.loadPDF(pdfBytes, "UserSecret2026")) {
            assertThat(doc.isEncrypted()).isTrue();
            AccessPermission permission = doc.getCurrentAccessPermission();
            assertThat(permission.canExtractContent()).isFalse();
            assertThat(permission.canPrint()).isTrue();
        }

        // 3. Vérifier que la génération a été consignée dans l'audit trail
        List<AuditLog> logs = auditLogRepository.findByCodeEntrepriseOrderByDateCreationDesc("SECURITY_CORP");
        assertThat(logs).isNotEmpty();
        AuditLog generateLog = logs.stream()
                .filter(l -> "DOCUMENT_GENERATE".equals(l.getAction()))
                .findFirst()
                .orElse(null);
        assertThat(generateLog).isNotNull();
        assertThat(generateLog.getUserEmail()).isEqualTo("admin@security.com");
        assertThat(generateLog.getDetailsJson()).contains("\"hasProtection\":true");
    }

    @Test
    @DisplayName("Refus de génération si signature et mot de passe sont demandés simultanément")
    void shouldRejectSimultaneousProtectionAndSignature() throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("nom_employe", "Jean Dupont");
        data.put("mot_de_passe_utilisateur", "UserSecret2026");
        data.put("signature_certificate_id", UUID.randomUUID().toString());

        mockMvc.perform(post("/api/templates/" + testTemplate.getId() + "/generate")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("simultanément")));
    }

    @Test
    @DisplayName("Génération d'un PDF avec signature numérique d'entreprise PAdES et audit log")
    void shouldGenerateDigitallySignedPdf() throws Exception {
        // Enregistrer le certificat en base
        CompanyCertificate cert = CompanyCertificate.builder()
                .codeEntreprise("SECURITY_CORP")
                .nom("Cachet Entreprise Test")
                .type("PKCS12")
                .fichierCertificatBase64(p12Base64)
                .motDePasseChiffre(aesCryptoService.encrypt(P12_PASS))
                .aliasCertificat("alias-security")
                .emetteur("CN=Security Corp Cachet")
                .sujet("CN=Security Corp Cachet")
                .dateExpiration(LocalDateTime.now().plusYears(1))
                .actif(true)
                .build();
        CompanyCertificate savedCert = companyCertificateRepository.save(cert);

        Map<String, Object> data = new HashMap<>();
        data.put("nom_employe", "Jean Dupont");
        data.put("signature_certificate_id", savedCert.getId().toString());
        data.put("nom_signataire", "Security Corp Direction");
        data.put("raison_signature", "Certification légale de fiche de paie");

        byte[] pdfBytes = mockMvc.perform(post("/api/templates/" + testTemplate.getId() + "/generate")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(pdfBytes).isNotEmpty();

        // Vérification de la signature dans le PDF
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertThat(doc.getSignatureDictionaries()).isNotEmpty();
            assertThat(doc.getSignatureDictionaries().get(0).getName()).isEqualTo("Security Corp Direction");
            assertThat(doc.getSignatureDictionaries().get(0).getReason()).isEqualTo("Certification légale de fiche de paie");
        }

        // Vérification dans l'audit trail
        List<AuditLog> logs = auditLogRepository.findByCodeEntrepriseOrderByDateCreationDesc("SECURITY_CORP");
        AuditLog signLog = logs.stream()
                .filter(l -> "DOCUMENT_GENERATE".equals(l.getAction()))
                .findFirst()
                .orElse(null);
        assertThat(signLog).isNotNull();
        assertThat(signLog.getDetailsJson()).contains("\"hasSignature\":true");
    }

    @Test
    @DisplayName("Téléchargement de document et consignation dans l'audit log")
    void shouldLogAuditOnDownload() throws Exception {
        Map<String, Object> data = Map.of("nom_employe", "Paul Martin");

        // 1. Génération
        mockMvc.perform(post("/api/templates/" + testTemplate.getId() + "/generate")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(data)))
                .andExpect(status().isOk());

        List<ReportGeneration> gens = generationRepository.findByTemplate_IdOrderByDateGenerationDesc(testTemplate.getId());
        assertThat(gens).isNotEmpty();
        UUID genId = gens.get(0).getId();

        // 2. Téléchargement via /api/generations/{id}/download
        mockMvc.perform(get("/api/generations/" + genId + "/download")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        // 3. Vérifier la présence de DOCUMENT_DOWNLOAD dans audit_log
        List<AuditLog> logs = auditLogRepository.findByCodeEntrepriseOrderByDateCreationDesc("SECURITY_CORP");
        AuditLog downloadLog = logs.stream()
                .filter(l -> "DOCUMENT_DOWNLOAD".equals(l.getAction()))
                .findFirst()
                .orElse(null);
        assertThat(downloadLog).isNotNull();
        assertThat(downloadLog.getRessourceId()).isEqualTo(genId.toString());
        assertThat(downloadLog.getUserEmail()).isEqualTo("admin@security.com");
    }
}