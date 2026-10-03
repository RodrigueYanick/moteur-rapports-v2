package com.rapports.moteur.service;

import com.rapports.moteur.dto.dtoCertificate.CertificateCreateRequest;
import com.rapports.moteur.dto.dtoCertificate.CertificateResponse;
import com.rapports.moteur.entity.CompanyCertificate;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.repository.CompanyCertificateRepository;
import com.rapports.moteur.security.crypto.AesCryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyCertificateService {

    private final CompanyCertificateRepository repository;
    private final EntrepriseService entrepriseService;
    private final AesCryptoService cryptoService;

    public List<CertificateResponse> listCertificates() {
        String codeEntreprise = entrepriseService.getCurrentCodeEntreprise();
        if (codeEntreprise == null || codeEntreprise.isBlank()) {
            return Collections.emptyList();
        }
        return repository.findByCodeEntrepriseOrderByDateCreationDesc(codeEntreprise).stream()
                .map(CertificateResponse::fromEntity)
                .toList();
    }

    public List<CertificateResponse> listActiveCertificates() {
        String codeEntreprise = entrepriseService.getCurrentCodeEntreprise();
        if (codeEntreprise == null || codeEntreprise.isBlank()) {
            return Collections.emptyList();
        }
        return repository.findByCodeEntrepriseAndActifTrueOrderByDateCreationDesc(codeEntreprise).stream()
                .map(CertificateResponse::fromEntity)
                .toList();
    }

    public CertificateResponse getCertificate(UUID id) {
        return CertificateResponse.fromEntity(findAndVerify(id));
    }

    @Transactional
    public CertificateResponse createCertificate(CertificateCreateRequest request) {
        String codeEntreprise = entrepriseService.getCurrentCodeEntreprise();
        if (codeEntreprise == null || codeEntreprise.isBlank()) {
            throw new ValidationException(List.of("Code entreprise manquant dans la session"));
        }

        if (request.getNom() == null || request.getNom().isBlank()) {
            throw new ValidationException(List.of("Le nom du certificat est obligatoire"));
        }
        if (request.getFichierBase64() == null || request.getFichierBase64().isBlank()) {
            throw new ValidationException(List.of("Le fichier certificat PKCS#12 encodé en base64 est obligatoire"));
        }

        if (repository.existsByCodeEntrepriseAndNom(codeEntreprise, request.getNom())) {
            throw new ValidationException(List.of("Un certificat avec ce nom existe déjà pour cette entreprise"));
        }

        byte[] certBytes;
        try {
            certBytes = Base64.getDecoder().decode(request.getFichierBase64().trim());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(List.of("Le fichier certificat fourni n'est pas un flux base64 valide"));
        }

        CertificateMetadata metadata = inspectAndValidatePkcs12(certBytes, request.getMotDePasse());
        String motDePasseChiffre = cryptoService.encrypt(request.getMotDePasse());

        CompanyCertificate certificate = CompanyCertificate.builder()
                .codeEntreprise(codeEntreprise)
                .nom(request.getNom().trim())
                .description(request.getDescription())
                .type("PKCS12")
                .fichierCertificatBase64(request.getFichierBase64().trim())
                .motDePasseChiffre(motDePasseChiffre)
                .aliasCertificat(metadata.alias())
                .emetteur(metadata.emetteur())
                .sujet(metadata.sujet())
                .dateExpiration(metadata.expiration())
                .actif(true)
                .build();

        CompanyCertificate saved = repository.save(certificate);
        log.info("Certificat d'entreprise '{}' crÃ©Ã© pour {}", saved.getNom(), codeEntreprise);
        return CertificateResponse.fromEntity(saved);
    }

    @Transactional
    public CertificateResponse toggleActive(UUID id, boolean actif) {
        CompanyCertificate cert = findAndVerify(id);
        cert.setActif(actif);
        return CertificateResponse.fromEntity(repository.save(cert));
    }

    @Transactional
    public void deleteCertificate(UUID id) {
        CompanyCertificate cert = findAndVerify(id);
        repository.delete(cert);
    }

    public DecryptedCertificate loadDecryptedCertificate(UUID id) {
        CompanyCertificate cert = findAndVerify(id);
        if (!cert.isActif()) {
            throw new ValidationException(List.of("Le certificat sÃ©lectionnÃ© est inactif"));
        }
        if (cert.getDateExpiration() != null && cert.getDateExpiration().isBefore(LocalDateTime.now())) {
            throw new ValidationException(List.of("Le certificat sÃ©lectionnÃ© a expirÃ©"));
        }
        byte[] bytes = Base64.getDecoder().decode(cert.getFichierCertificatBase64());
        String plainPassword = cryptoService.decrypt(cert.getMotDePasseChiffre());
        return new DecryptedCertificate(bytes, plainPassword, cert.getAliasCertificat());
    }

    private CompanyCertificate findAndVerify(UUID id) {
        String codeEntreprise = entrepriseService.getCurrentCodeEntreprise();
        return repository.findByIdAndCodeEntreprise(id, codeEntreprise)
                .orElseThrow(() -> new ValidationException(List.of("Certificat introuvable ou accÃ¨s non autorisÃ©")));
    }

    private CertificateMetadata inspectAndValidatePkcs12(byte[] p12Bytes, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new ByteArrayInputStream(p12Bytes), password.toCharArray());
            Enumeration<String> aliases = keyStore.aliases();
            String foundAlias = null;
            X509Certificate foundCert = null;

            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (keyStore.isKeyEntry(alias)) {
                    java.security.cert.Certificate cert = keyStore.getCertificate(alias);
                    if (cert instanceof X509Certificate x509) {
                        foundAlias = alias;
                        foundCert = x509;
                        break;
                    }
                }
            }
            if (foundAlias == null || foundCert == null) {
                throw new ValidationException(List.of("Le fichier PKCS#12 ne contient aucune clÃ© privÃ©e et certificat X.509 valides"));
            }
            LocalDateTime expiration = foundCert.getNotAfter().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            return new CertificateMetadata(foundAlias, foundCert.getIssuerX500Principal().getName(), foundCert.getSubjectX500Principal().getName(), expiration);
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            throw new ValidationException(List.of("Ã‰chec de dÃ©chiffrement du trousseau PKCS#12 : mot de passe incorrect ou format invalide"));
        }
    }

    public record CertificateMetadata(String alias, String emetteur, String sujet, LocalDateTime expiration) {}
    public record DecryptedCertificate(byte[] pkcs12Bytes, String password, String alias) {}
}