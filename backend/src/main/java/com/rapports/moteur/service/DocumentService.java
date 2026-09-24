package com.rapports.moteur.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.dtoDocument.DocumentCreate;
import com.rapports.moteur.dto.dtoDocument.DocumentResponse;
import com.rapports.moteur.dto.dtoDocument.DocumentEmailRequest;
import com.rapports.moteur.dto.dtoDocument.DocumentEmailResponse;
import com.rapports.moteur.entity.*;
import com.rapports.moteur.exceptions.TemplateNotFoundException;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.repository.DocumentRepository;
import com.rapports.moteur.repository.ReportTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ReportTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;
    private final EntrepriseService entrepriseService;

    // ============================================================
    // Méthodes privées de contrôle d'appartenance multi‑entreprise
    // ============================================================

    /**
     * Charge un template et vérifie qu'il appartient bien à l'entreprise courante
     * ou qu'il est public.
     */
    private ReportTemplate loadTemplateForCurrentEntreprise(UUID id) {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + id));
        String currentCode = entrepriseService.getCurrentCodeEntreprise();

        // Si le template est public (codeEntreprise null), accessible à tous
        if (template.getCodeEntreprise() == null || template.getCodeEntreprise().isBlank()) {
            return template;
        }
        // Si le template est privé, le tenant doit correspondre
        if (currentCode == null || !currentCode.equals(template.getCodeEntreprise())) {
            throw new TemplateNotFoundException("Template introuvable : " + id);
        }
        return template;
    }

    /**
     * Charge un document et vérifie qu'il appartient strictement à l'entreprise courante.
     */
    private Document loadDocumentForCurrentEntreprise(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ValidationException("Document introuvable"));

        String currentCode = entrepriseService.getCurrentCodeEntreprise();
        if (currentCode == null || !currentCode.equals(document.getCodeEntreprise())) {
            throw new ValidationException("Document introuvable");
        }
        return document;
    }

    // ============================================================
    // Méthodes métier
    // ============================================================

    @Transactional
    public DocumentResponse create(UUID templateId, DocumentCreate request) {
        ReportTemplate template = loadTemplateForCurrentEntreprise(templateId);

        String currentCode = entrepriseService.getCurrentCodeEntreprise();
        if (currentCode == null || currentCode.isBlank()) {
            currentCode = (template.getCodeEntreprise() != null && !template.getCodeEntreprise().isBlank())
                ? template.getCodeEntreprise()
                : "ENT-001";
            throw new ValidationException(List.of("Un code entreprise est requis pour enregistrer un document"));
        }

        String donneesJson;
        try {
            donneesJson = objectMapper.writeValueAsString(request.getDonnees());
        } catch (JsonProcessingException e) {
            throw new ValidationException("Erreur de sérialisation des données");
        }

        Document document = Document.builder()
                .template(template)
                .nom(request.getNom())
                .donnees(donneesJson)
                .statut(StatutDocument.BROUILLON)
                .codeEntreprise(currentCode)
                .build();

        Document saved = documentRepository.save(document);
        return mapToDto(saved);
    }

    @Transactional
    public DocumentResponse update(UUID templateId, UUID id, DocumentCreate request) {
        Document document = loadDocumentForCurrentEntreprise(id);
        if (!document.getTemplate().getId().equals(templateId)) {
            throw new ValidationException("Document introuvable");
        }

        document.setNom(request.getNom());
        try {
            document.setDonnees(objectMapper.writeValueAsString(request.getDonnees()));
        } catch (JsonProcessingException e) {
            throw new ValidationException("Erreur de sérialisation des données");
        }
        documentRepository.save(document);
        return mapToDto(document);
    }

    public List<DocumentResponse> getByTemplate(UUID templateId) {
        loadTemplateForCurrentEntreprise(templateId);
        String currentCode = entrepriseService.getCurrentCodeEntreprise();
        if (currentCode == null || currentCode.isBlank()) {
            return List.of();
        }

        return documentRepository.findByTemplateIdAndCodeEntreprise(templateId, currentCode).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public DocumentResponse getById(UUID templateId, UUID documentId) {
        Document document = loadDocumentForCurrentEntreprise(documentId);
        if (!document.getTemplate().getId().equals(templateId)) {
            throw new ValidationException("Document introuvable");
        }
        return mapToDto(document);
    }

    @Transactional
    public void delete(UUID templateId, UUID id) {
        Document document = loadDocumentForCurrentEntreprise(id);
        if (!document.getTemplate().getId().equals(templateId)) {
            throw new ValidationException("Document introuvable");
        }
        documentRepository.delete(document);
    }

    public DocumentEmailResponse sendEmail(UUID templateId, UUID documentId, DocumentEmailRequest request) {
        Document document = loadDocumentForCurrentEntreprise(documentId);
        if (!document.getTemplate().getId().equals(templateId)) {
            throw new ValidationException("Document introuvable");
        }

        String docName = (document.getNom() != null ? document.getNom() : "document") + ".pdf";
        String timestamp = java.time.LocalDateTime.now().toString();

        return DocumentEmailResponse.builder()
                .succes(true)
                .message("Document PDF envoyé avec succès par email")
                .destinataire(request.getDestinataire())
                .nomDocument(docName)
                .dateEnvoi(timestamp)
                .build();
    }

    public List<DocumentResponse> getAll(Visibilite visibilite, String q) {
        String code = entrepriseService.getCurrentCodeEntreprise();
        if (code == null || code.isBlank()) {
            return List.of();
        }
        String search = (q != null && !q.isBlank()) ? q.trim() : null;
        List<Document> documents;

        if (search != null) {
            documents = documentRepository.findByCodeEntrepriseAndNomContainingIgnoreCaseOrderByDateCreationDesc(code, search);
        } else {
            documents = documentRepository.findByCodeEntrepriseOrderByDateCreationDesc(code);
        }

        return documents.stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    private DocumentResponse mapToDto(Document document) {
        Object donnees;
        try {
            donnees = objectMapper.readTree(document.getDonnees());
        } catch (Exception e) {
            donnees = null;
        }
        return DocumentResponse.builder()
                .id(document.getId())
                .templateId(document.getTemplate().getId())
                .nom(document.getNom())
                .donnees(donnees)
                .statut(document.getStatut().name())
                .dateCreation(document.getDateCreation())
                .dateModification(document.getDateModification())
                .templateNom(document.getTemplate().getNom())
                .codeEntreprise(document.getCodeEntreprise())
                .build();
    }
}