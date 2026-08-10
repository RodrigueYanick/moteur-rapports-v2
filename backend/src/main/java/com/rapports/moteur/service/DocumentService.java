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

    @Transactional
    public DocumentResponse create(UUID templateId, DocumentCreate request) {
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable"));

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
    public DocumentResponse update(UUID id, DocumentCreate request) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Document introuvable"));

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
        return documentRepository.findByTemplateId(templateId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public DocumentResponse getById(UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Document introuvable"));
        return mapToDto(document);
    }

    @Transactional
    public void delete(UUID id) {
        documentRepository.deleteById(id);
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


    // (ajoute le paramètre au constructeur existant)

    public List<DocumentResponse> getAll() {
        String code = entrepriseService.getCurrentCodeEntreprise();
        List<Document> documents = documentRepository.findByCodeEntreprise(code);
        // Filtre les documents dont le template appartient au code entreprise
        return documents.stream()
                .filter(doc -> doc.getTemplate().getCodeEntreprise().equals(code))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
}