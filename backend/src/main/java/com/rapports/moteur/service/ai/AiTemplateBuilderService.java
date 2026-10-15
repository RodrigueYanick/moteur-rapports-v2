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

        CompanyWorkspaceConfig defaultCfg = workspaceConfigService.getEntityForCurrentEntreprise();
        String contextualPrompt = buildContextualPrompt(request, defaultCfg);

        AtomicBoolean usedMock = new AtomicBoolean(false);
        Map<String, Object> responseSchema = buildTemplateResponseSchema();
        String rawJson = aiClientManager.executeWithFallback(
                SYSTEM_INSTRUCTION,
                contextualPrompt,
                true,
                null,
                responseSchema,
                usedMock
        );

        String cleanedJson = sanitizeJson(rawJson);

        JsonNode root;
        try {
            root = objectMapper.readTree(cleanedJson);
        } catch (Exception e) {
            log.error("Échec de parsing JSON du modèle IA : {}", cleanedJson, e);
            throw new AiException("Le modèle IA a renvoyé un format JSON invalide : " + e.getMessage(), e);
        }

        root = postProcessAndNormalizeDesign(root);

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

    private String buildContextualPrompt(AiTemplatePromptRequest request, CompanyWorkspaceConfig defaultCfg) {
        StringBuilder sb = new StringBuilder();
        sb.append("PROMPT UTILISATEUR : \n").append(request.getPrompt()).append("\n\n");
        sb.append("CONTRAINTES GÉOMÉTRIQUES ET CHARTE GRAPHIQUE :\n");
        sb.append("- Format de papier cible : ").append(request.getFormatPapier() != null ? request.getFormatPapier() : "A4").append("\n");
        sb.append("- Largeur utile de la page : 794 pixels (zone imprimable : x de 40 à 754 px)\n");
        if (defaultCfg != null && defaultCfg.getCouleurFond() != null) {
            sb.append("- Couleur de fond par défaut : ").append(defaultCfg.getCouleurFond()).append("\n");
        }
        sb.append("- Règle de non-chevauchement vertical : chaque bloc successif doit avoir un 'y' strictement supérieur au 'y' précédent + sa hauteur estimée (au minimum +30px pour du texte, +120px pour un tableau).\n");
        sb.append("- Utiliser des balises Mustache standard {{nom_variable}} en minuscules et sans espaces.\n\n");

        String lower = request.getPrompt().toLowerCase();
        if (lower.contains("paie") || lower.contains("salaire") || "RH".equalsIgnoreCase(request.getCategorie())) {
            sb.append("""
            EXEMPLE DE STRUCTURE RH ATTENDUE :
            - Titre : BULLETIN DE PAIE - {{periode}} (y=40)
            - Émetteur (y=100) : {{entreprise_nom}}, SIRET {{entreprise_siret}}
            - Salarié (y=100, x=420) : {{employe_nom}}, Poste : {{employe_poste}}, Matricule : {{employe_matricule}}
            - Tableau des cotisations (y=220) avec source="{{cotisations}}" et colonnes : Rubrique, Base, Taux, Montant
            - Totaux (y=520) : Salaire Brut {{salaire_brut}}, Net Imposable {{net_imposable}}, Net à Payer {{net_a_payer}} €
            """);
        } else {
            sb.append("""
            EXEMPLE DE STRUCTURE VENTES/FACTURATION ATTENDUE :
            - En-tête (y=40) : TITRE DU DOCUMENT N° {{numero_document}}
            - Émetteur (y=100, x=40, w=340) : {{entreprise_nom}}, {{entreprise_adresse}}, SIRET {{entreprise_siret}}
            - Client (y=100, x=420, w=334) : {{client_nom}}, {{client_adresse}}, Email {{client_email}}
            - Métadonnées (y=200, x=40) : Date d'émission {{date_emission}} | Échéance {{date_echeance}}
            - Tableau dynamique (y=250, x=40, w=714) : source="{{lignes}}" avec colonnes : Désignation, Quantité, Prix unitaire, Total HT
            - Récapitulatif financier (y=500, x=450, w=304) : Total HT {{total_ht}} €, TVA {{montant_tva}} €, Total TTC {{total_ttc}} €
            - Modalités (y=640, x=40, w=714) : Conditions de règlement, IBAN {{entreprise_iban}}
            """);
        }
        return sb.toString();
    }

    private Map<String, Object> buildTemplateResponseSchema() {
        Map<String, Object> colItem = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "titre", Map.of("type", "STRING"),
                        "variable", Map.of("type", "STRING")
                ),
                "required", List.of("titre", "variable")
        );

        Map<String, Object> styleProps = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "fontSize", Map.of("type", "NUMBER"),
                        "bold", Map.of("type", "BOOLEAN"),
                        "color", Map.of("type", "STRING"),
                        "align", Map.of("type", "STRING")
                )
        );

        Map<String, Object> blocItem = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "id", Map.of("type", "STRING"),
                        "type", Map.of("type", "STRING"),
                        "x", Map.of("type", "NUMBER"),
                        "y", Map.of("type", "NUMBER"),
                        "largeurBox", Map.of("type", "NUMBER"),
                        "contenu", Map.of("type", "STRING"),
                        "source", Map.of("type", "STRING"),
                        "colonnes", Map.of("type", "ARRAY", "items", colItem),
                        "style", styleProps
                ),
                "required", List.of("id", "type", "x", "y", "largeurBox", "contenu")
        );

        Map<String, Object> pageItem = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "nom", Map.of("type", "STRING"),
                        "blocs", Map.of("type", "ARRAY", "items", blocItem)
                ),
                "required", List.of("nom", "blocs")
        );

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("nom", Map.of("type", "STRING"));
        props.put("description", Map.of("type", "STRING"));
        props.put("categorie", Map.of("type", "STRING"));
        props.put("formatPapier", Map.of("type", "STRING"));
        props.put("pages", Map.of("type", "ARRAY", "items", pageItem));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "OBJECT");
        schema.put("properties", props);
        schema.put("required", List.of("nom", "formatPapier", "pages"));
        return schema;
    }

    private JsonNode postProcessAndNormalizeDesign(JsonNode root) {
        if (!root.isObject()) return root;
        com.fasterxml.jackson.databind.node.ObjectNode rootObj = (com.fasterxml.jackson.databind.node.ObjectNode) root;
        JsonNode pagesNode = rootObj.path("pages");
        if (!pagesNode.isArray() || pagesNode.isEmpty()) {
            return root;
        }

        int blockCounter = 1;
        for (JsonNode pageNode : pagesNode) {
            JsonNode blocsNode = pageNode.path("blocs");
            if (blocsNode.isArray()) {
                for (JsonNode bloc : blocsNode) {
                    if (bloc.isObject()) {
                        com.fasterxml.jackson.databind.node.ObjectNode b = (com.fasterxml.jackson.databind.node.ObjectNode) bloc;
                        if (!b.hasNonNull("id") || b.get("id").asText().isBlank()) {
                            b.put("id", "b_auto_" + (blockCounter++));
                        }
                        int x = b.path("x").asInt(40);
                        int y = b.path("y").asInt(40);
                        int w = b.path("largeurBox").asInt(714);

                        x = Math.max(20, Math.min(x, 700));
                        y = Math.max(20, Math.min(y, 1100));
                        w = Math.max(60, Math.min(w, 754));
                        if (x + w > 754) {
                            w = 754 - x;
                        }
                        b.put("x", x);
                        b.put("y", y);
                        b.put("largeurBox", w);

                        String type = b.path("type").asText("texte").toLowerCase();
                        if (!List.of("titre", "texte", "tableau", "rectangle", "separateur", "image").contains(type)) {
                            type = "texte";
                        }
                        b.put("type", type);

                        if (b.has("contenu")) {
                            String contenu = b.get("contenu").asText("");
                            contenu = contenu.replaceAll("\\{\\{\\s+([a-zA-Z0-9_]+)\\s+\\}\\}", "{{$1}}");
                            b.put("contenu", contenu);
                        }
                    }
                }
            }
        }
        return rootObj;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
