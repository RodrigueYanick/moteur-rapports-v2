package com.rapports.moteur.controller;

import com.rapports.moteur.dto.dtoCertificate.CertificateCreateRequest;
import com.rapports.moteur.dto.dtoCertificate.CertificateResponse;
import com.rapports.moteur.service.CompanyCertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/certificates")
@RequiredArgsConstructor
@Tag(name = "Certificats & Cachets d'Entreprise", description = "Gestion des certificats cryptographiques X.509 / PKCS#12 pour la signature numérique PAdES")
public class CompanyCertificateController {

    private final CompanyCertificateService certificateService;

    @Operation(summary = "Lister les certificats de l'entreprise", description = "Retourne tous les certificats (actifs et inactifs)")
    @GetMapping
    public ResponseEntity<List<CertificateResponse>> listCertificates() {
        return ResponseEntity.ok(certificateService.listCertificates());
    }

    @Operation(summary = "Lister les certificats actifs de l'entreprise", description = "Retourne uniquement les certificats actifs et exploitables pour la signature")
    @GetMapping("/active")
    public ResponseEntity<List<CertificateResponse>> listActiveCertificates() {
        return ResponseEntity.ok(certificateService.listActiveCertificates());
    }

    @Operation(summary = "Consulter les détails d'un certificat", description = "Retourne les métadonnées et la date d'expiration")
    @GetMapping("/{id}")
    public ResponseEntity<CertificateResponse> getCertificate(@PathVariable UUID id) {
        return ResponseEntity.ok(certificateService.getCertificate(id));
    }

    @Operation(summary = "Importer un certificat d'entreprise PKCS#12", description = "Valide le trousseau .p12/.pfx, extrait les métadonnées et chiffre le mot de passe en AES-256-GCM")
    @PostMapping
    public ResponseEntity<CertificateResponse> createCertificate(@Valid @RequestBody CertificateCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(certificateService.createCertificate(request));
    }

    @Operation(summary = "Activer ou désactiver un certificat", description = "Bascule le statut actif du certificat")
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<CertificateResponse> toggleActive(
            @PathVariable UUID id,
            @RequestParam boolean actif) {
        return ResponseEntity.ok(certificateService.toggleActive(id, actif));
    }

    @Operation(summary = "Supprimer un certificat", description = "Supprime définitivement un certificat de l'entreprise")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCertificate(@PathVariable UUID id) {
        certificateService.deleteCertificate(id);
        return ResponseEntity.noContent().build();
    }
}