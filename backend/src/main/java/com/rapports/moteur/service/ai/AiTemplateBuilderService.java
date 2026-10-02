package com.rapports.moteur.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.dtoAi.AiTemplateGenerationResponse;
import com.rapports.moteur.dto.dtoAi.AiTemplatePromptRequest;
import com.rapports.moteur.dto.dtoVariable.ExtractedVariable;
import com.rapports.moteur.entity.*;
import com.rapports.moteur.exceptions.AiException;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.repository.ReportVariableRepository;
import com.rapports.moteur.service.CompanyWorkspaceConfigService;
import com.rapports.moteur.service.EntrepriseService;
import com.rapports.moteur.service.SchemaExtractorService;
import com.rapports.moteur.service.audit.AuditTrailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTemplateBuilderService {

    private final ReportTemplateRepository templateRepository;
    private final ReportVariableRepository variableRepository;
    private final SchemaExtractorService schemaExtractorService;
    private final EntrepriseService entrepriseService;
    private final CompanyWorkspaceConfigService workspaceConfigService;
    private final AuditTrailService auditTrailService;
    private final AiClientManager aiClientManager;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_INSTRUCTION = """
        Tu es un expert mondial en design de rapports d'entreprise, factures, devis, bons de commande et tableaux de bord d'entreprise.
        Ton objectif est de générer une structure JSON de modèle de document parfaitement valide pour le moteur de rapports.

        Règles de structure JSON obligatoire :
        Le document JSON retourné doit respecter STRICTEMENT ce schéma JSON :
        {
          "nom": "Nom du modèle",
          "description": "Description concise",
          "categorie": "VENTES",
          "formatPapier": "A4",
          "pages": [
            {
              "nom": "Page 1",
              "blocs": [ ... ]
            }
          ]
        }

        Règles pour les blocs de pages[].blocs :
        Chaque bloc a les attributs suivants :
        - id: identifiant unique court (ex: "b_titre_1", "b_emetteur_2", "b_tab_3", etc.)
        - type: "titre", "texte", "tableau", "rectangle", "separateur", "image"
        - x: position horizontale en pixels (entre 40 et 754 pour un A4 de largeur 794)
        - y: position verticale en pixels (espacement vertical harmonieux et sans chevauchement)
        - largeurBox: largeur du bloc (ex: 714 pour pleine largeur, ou 340 pour 2 colonnes)
        - contenu: texte ou balises HTML simples <b>, <i>, <br>, <span>.
          Pour insérer des variables dynamiques, utilise le formalisme Mustache : {{nom_variable}}
        - style: objet JSON optionnel { "fontSize": 12, "bold": true, "color": "#1e3a8a", "align": "left" | "center" | "right" }
        - Pour un bloc "tableau" dynamique :
          - type: "tableau"
          - source: "{{lignes}}"
          - colonnes: [
              { "titre": "Libellé colonne", "variable": "nom_colonne" }
            ]

        Disposition recommandée pour un devis ou facture :
        - y=40 : Titre ou type de document (taille 22-26px, gras, couleur d'accentuation)
        - y=100..180 : Informations émetteur (gauche, x=40, w=340) et destinataire/client (droite, x=420, w=334) avec variables {{entreprise_nom}}, {{client_nom}}, etc.
        - y=200..230 : Métadonnées du document : Date d'émission {{date_emission}}, Référence / N° {{numero_document}}
        - y=250 : Tableau dynamique des articles ou prestations avec source "{{lignes}}"
        - y=500..600 : Récapitulatif financier (aligné à droite) : Total HT {{total_ht}} €, TVA {{montant_tva}} €, Total TTC {{total_ttc}} €
        - y=650..700 : Modalités de règlement (IBAN, délais, conditions)

        IMPORTANT :
        - Tu ne dois répondre QUE par du JSON valide, sans texte d'introduction ni de conclusion, sans backticks markdown.
        """;

    @Transactional
    public AiTemplateGenerationResponse generateTemplate(AiTemplatePromptRequest request) {
        if (request.getPrompt() == null || request.getPrompt().isBlank()) {
            throw new IllegalArgumentException("Le prompt utilisateur ne peut pas être vide");
        }

        AtomicBoolean usedMock = new AtomicBoolean(false);
        String rawJson = aiClientManager.executeWithFallback(SYSTEM_INSTRUCTION, request.getPrompt(), true, usedMock);

        String cleanedJson = sanitizeJson(rawJson);

        JsonNode root;
        try {
            root = objectMapper.readTree(cleanedJson);
        } catch (Exception e) {
            log.error("Échec de parsing JSON du modèle IA : {}", cleanedJson, e);
            throw new AiException("Le modèle IA a renvoyé un format JSON invalide : " + e.getMessage(), e);
        }

        String nom = request.getNom() != null && !request.getNom().isBlank() 
                ? request.getNom() 
                : root.path("nom").asText("Modèle IA " + UUID.randomUUID().toString().substring(0, 6));

        String description = root.path("description").asText("Généré par IA à partir du prompt : " + request.getPrompt());
        String formatPapier = request.getFormatPapier() != null && !request.getFormatPapier().isBlank()
                ? request.getFormatPapier()
                : root.path("formatPapier").asText("A4");

        Categorie categorie = determineCategorie(request.getCategorie(), root.path("categorie").asText("VENTES"));

        // Extraire la partie pages
        JsonNode pagesNode = root.path("pages");
        if (pagesNode.isMissingNode() || !pagesNode.isArray() || pagesNode.isEmpty()) {
            throw new AiException("La réponse de l'IA ne contient aucune page valide");
        }

        Map<String, Object> designMap = new LinkedHashMap<>();
        designMap.put("pages", pagesNode);
        String contenuDesign;
        try {
            contenuDesign = objectMapper.writeValueAsString(designMap);
        } catch (Exception e) {
            throw new AiException("Erreur de sérialisation du design généré : " + e.getMessage(), e);
        }

        // Créer l'entité ReportTemplate
        ReportTemplate template = new ReportTemplate();
        template.setNom(nom);
        template.setDescription(description);
        template.setFormatPapier(formatPapier);
        template.setCategorie(categorie);
        template.setContenuDesign(contenuDesign);
        template.setStatut(TemplateStatus.BROUILLON);
        template.setVersion(1);
        template.setModePagination(PaginationMode.FIXED);

        String currentCodeEntreprise = entrepriseService.getCurrentCodeEntreprise();
        template.setCodeEntreprise(currentCodeEntreprise);

        // Héritage des options de workspace par défaut
        CompanyWorkspaceConfig defaultCfg = workspaceConfigService.getEntityForCurrentEntreprise();
        template.setLargeurMm(defaultCfg != null && defaultCfg.getLargeurMm() != null ? defaultCfg.getLargeurMm() : 210);
        template.setHauteurMm(defaultCfg != null && defaultCfg.getHauteurMm() != null ? defaultCfg.getHauteurMm() : 297);
        template.setMargeGaucheMm(defaultCfg != null && defaultCfg.getMargeGaucheMm() != null ? defaultCfg.getMargeGaucheMm() : 10);
        template.setMargeDroiteMm(defaultCfg != null && defaultCfg.getMargeDroiteMm() != null ? defaultCfg.getMargeDroiteMm() : 10);
        template.setMargeHautMm(defaultCfg != null && defaultCfg.getMargeHautMm() != null ? defaultCfg.getMargeHautMm() : 10);
        template.setMargeBasMm(defaultCfg != null && defaultCfg.getMargeBasMm() != null ? defaultCfg.getMargeBasMm() : 10);
        template.setCouleurFond(defaultCfg != null && defaultCfg.getCouleurFond() != null ? defaultCfg.getCouleurFond() : "#ffffff");

        ReportTemplate savedTemplate = templateRepository.save(template);

        // Extraction et sauvegarde des variables détectées
        List<ExtractedVariable> extractedVars = schemaExtractorService.extract(contenuDesign);
        List<String> varNames = new ArrayList<>();
        List<ReportVariable> variablesToSave = new ArrayList<>();

        for (ExtractedVariable ev : extractedVars) {
            varNames.add(ev.getNom());
            VariableType vType = parseVariableType(ev.getType());
            variablesToSave.add(ReportVariable.builder()
                    .template(savedTemplate)
                    .nomVariable(ev.getNom())
                    .type(vType)
                    .obligatoire(Boolean.TRUE.equals(ev.getObligatoire()))
                    .description("Variable générée automatiquement par l'IA")
                    .build());
        }
        if (!variablesToSave.isEmpty()) {
            variableRepository.saveAll(variablesToSave);
        }

        // Piste d'audit
        auditTrailService.log(
                "AI_TEMPLATE_GENERATE",
                "ReportTemplate",
                savedTemplate.getId().toString(),
                String.format("{\"prompt\":\"%s\",\"variablesCount\":%d,\"fromMock\":%b}",
                        escapeJson(request.getPrompt()), varNames.size(), usedMock.get()),
                "SUCCESS"
        );

        return AiTemplateGenerationResponse.builder()
                .templateId(savedTemplate.getId())
                .nom(savedTemplate.getNom())
                .description(savedTemplate.getDescription())
                .categorie(savedTemplate.getCategorie().name())
                .formatPapier(savedTemplate.getFormatPapier())
                .contenuDesign(savedTemplate.getContenuDesign())
                .extractedVariables(varNames)
                .promptUsed(request.getPrompt())
                .fromMock(usedMock.get())
                .provider(usedMock.get() ? "mock" : aiClientManager.getCurrentProvider())
                .build();
    }

    private String sanitizeJson(String raw) {
        if (raw == null) return "{}";
        String s = raw.trim();
        if (s.startsWith("```json")) {
            s = s.substring(7);
        } else if (s.startsWith("```")) {
            s = s.substring(3);
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }

    private Categorie determineCategorie(String requested, String fromAi) {
        if (requested != null && !requested.isBlank()) {
            try {
                return Categorie.valueOf(requested.toUpperCase());
            } catch (Exception ignored) {}
        }
        if (fromAi != null && !fromAi.isBlank()) {
            try {
                return Categorie.valueOf(fromAi.toUpperCase());
            } catch (Exception ignored) {}
        }
        return Categorie.VENTES;
    }

    private VariableType parseVariableType(String typeStr) {
        if (typeStr == null) return VariableType.STRING;
        try {
            return VariableType.valueOf(typeStr.toUpperCase());
        } catch (Exception e) {
            return VariableType.STRING;
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
