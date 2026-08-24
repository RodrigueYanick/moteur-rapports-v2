package com.rapports.moteur.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.dtoDocument.DocumentCreate;
import com.rapports.moteur.dto.dtoDocument.DocumentResponse;
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
     * Charge un template et vérifie qu'il appartient bien à l'entreprise courante.
     * Lève une TemplateNotFoundException si le template n'existe pas ou s'il
     * appartient à une autre entreprise (on ne révèle jamais l'existence d'un
     * template étranger).
     */
    private ReportTemplate loadTemplateForCurrentEntreprise(UUID id) {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + id));
        String currentCode = entrepriseService.getCurrentCodeEntreprise();
        
        // Si le template est public (codeEntreprise null), accessible à tous
        if (template.getCodeEntreprise() == null || template.getCodeEntreprise().isBlank()) {
            return template;
        }
        // Si le template est privé, le header doit correspondre
        if (currentCode == null || !currentCode.equals(template.getCodeEntreprise())) {
            throw new TemplateNotFoundException("Template introuvable : " + id);
        }
        return template;
    }

    /**
     * Charge un document et vérifie qu'il appartient à l'entreprise courante
     * via le code entreprise de son template.
     * Lève une ValidationException si le document n'existe pas ou n'appartient
     * pas à l'entreprise courante.
     */
    private Document loadDocumentForCurrentEntreprise(UUID documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ValidationException("Document introuvable"));

        String currentCode = entrepriseService.getCurrentCodeEntreprise();
        if (currentCode == null || !currentCode.equals(document.getTemplate().getCodeEntreprise())) {
            throw new ValidationException("Document introuvable");
        }
        return document;
    }

    // ============================================================
    // Méthodes métier
    // ============================================================

    @Transactional
    public DocumentResponse create(UUID templateId, DocumentCreate request) {
        // Vérifie que le template appartient à l'entreprise courante
        ReportTemplate template = loadTemplateForCurrentEntreprise(templateId);

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
        // Vérifie d'abord que le template est accessible
        loadTemplateForCurrentEntreprise(templateId);

        return documentRepository.findByTemplateId(templateId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public DocumentResponse getById(UUID templateId, UUID documentId) {
        Document document = loadDocumentForCurrentEntreprise(documentId);

        // Vérifie que le document appartient bien au template donné
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

    public List<DocumentResponse> getAll() {
        String code = entrepriseService.getCurrentCodeEntreprise();
        List<Document> documents = documentRepository.findByCodeEntreprise(code);
        return documents.stream()
                .filter(doc -> doc.getTemplate().getCodeEntreprise().equals(code))
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
                .build();
    }
}