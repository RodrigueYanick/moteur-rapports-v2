package com.rapports.moteur.service;

import com.rapports.moteur.dto.dtoCertificate.CertificateCreateRequest;
import com.rapports.moteur.dto.dtoCertificate.CertificateResponse;
import com.rapports.moteur.entity.CompanyCertificate;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.repository.CompanyCertificateRepository;
import com.rapports.moteur.security.crypto.AesCryptoService;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyCertificateServiceTest {

    @Mock
    private CompanyCertificateRepository repository;

    @Mock
    private EntrepriseService entrepriseService;

    @Mock
    private AesCryptoService cryptoService;

    @InjectMocks
    private CompanyCertificateService certificateService;

    private String validPkcs12Base64;
    private static final String P12_PASS = "TestSecret2026!";
    private static final String CODE_ENTREPRISE = "ENT-001";

    @BeforeEach
    void setUp() throws Exception {
        // Génération d'un PKCS#12 auto-signé valide en mémoire
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        X500Name subject = new X500Name("CN=ENTREPRISE CERT, O=RY TECH, C=FR");
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
        ks.setKeyEntry("alias-p12", kp.getPrivate(), P12_PASS.toCharArray(), new Certificate[]{cert});

        ByteArrayOutputStream p12Out = new ByteArrayOutputStream();
        ks.store(p12Out, P12_PASS.toCharArray());
        validPkcs12Base64 = Base64.getEncoder().encodeToString(p12Out.toByteArray());
    }

    @Test
    @DisplayName("Création réussie d'un certificat d'entreprise avec validation PKCS#12 et chiffrement AES")
    void shouldCreateCertificateSuccessfully() {
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn(CODE_ENTREPRISE);
        when(repository.existsByCodeEntrepriseAndNom(CODE_ENTREPRISE, "Cachet Officiel")).thenReturn(false);
        when(cryptoService.encrypt(P12_PASS)).thenReturn("ENC_PASS_123");
        when(repository.save(any(CompanyCertificate.class))).thenAnswer(invocation -> {
            CompanyCertificate c = invocation.getArgument(0);
            c.setId(UUID.randomUUID());
            c.setDateCreation(LocalDateTime.now());
            return c;
        });

        CertificateCreateRequest request = CertificateCreateRequest.builder()
                .nom("Cachet Officiel")
                .description("Cachet pour fiches de paie")
                .fichierBase64(validPkcs12Base64)
                .motDePasse(P12_PASS)
                .build();

        CertificateResponse response = certificateService.createCertificate(request);

        assertThat(response).isNotNull();
        assertThat(response.getNom()).isEqualTo("Cachet Officiel");
        assertThat(response.getCodeEntreprise()).isEqualTo(CODE_ENTREPRISE);
        assertThat(response.getAliasCertificat()).isEqualTo("alias-p12");
        assertThat(response.isActif()).isTrue();

        verify(cryptoService).encrypt(P12_PASS);
        verify(repository).save(any(CompanyCertificate.class));
    }

    @Test
    @DisplayName("Rejet de création si le mot de passe PKCS#12 est invalide")
    void shouldRejectInvalidPkcs12Password() {
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn(CODE_ENTREPRISE);
        when(repository.existsByCodeEntrepriseAndNom(CODE_ENTREPRISE, "Bad Cert")).thenReturn(false);

        CertificateCreateRequest request = CertificateCreateRequest.builder()
                .nom("Bad Cert")
                .fichierBase64(validPkcs12Base64)
                .motDePasse("WRONG_PASSWORD")
                .build();

        assertThatThrownBy(() -> certificateService.createCertificate(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("mot de passe incorrect");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Chargement et déchiffrement d'un certificat pour signature")
    void shouldLoadDecryptedCertificate() {
        UUID certId = UUID.randomUUID();
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn(CODE_ENTREPRISE);

        CompanyCertificate entity = CompanyCertificate.builder()
                .id(certId)
                .codeEntreprise(CODE_ENTREPRISE)
                .nom("Cachet RH")
                .fichierCertificatBase64(validPkcs12Base64)
                .motDePasseChiffre("ENC_SECRET")
                .aliasCertificat("alias-p12")
                .dateExpiration(LocalDateTime.now().plusYears(1))
                .actif(true)
                .build();

        when(repository.findByIdAndCodeEntreprise(certId, CODE_ENTREPRISE)).thenReturn(Optional.of(entity));
        when(cryptoService.decrypt("ENC_SECRET")).thenReturn(P12_PASS);

        CompanyCertificateService.DecryptedCertificate decrypted = certificateService.loadDecryptedCertificate(certId);

        assertThat(decrypted).isNotNull();
        assertThat(decrypted.password()).isEqualTo(P12_PASS);
        assertThat(decrypted.alias()).isEqualTo("alias-p12");
        assertThat(decrypted.pkcs12Bytes()).isNotEmpty();
    }
}