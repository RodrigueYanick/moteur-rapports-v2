package com.rapports.moteur.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rapports.moteur.dto.dtoAi.AiPersonaGenerationResponse;
import com.rapports.moteur.dto.dtoAi.AiPersonaPromptRequest;
import com.rapports.moteur.entity.Categorie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPersonaTemplateService {

    private final AiClientManager aiClientManager;
    private final ObjectMapper objectMapper;

    public AiPersonaGenerationResponse generatePersonaTemplate(AiPersonaPromptRequest request) {
        String personaType = normalizePersonaType(request.getPersonaType());
        log.info("Génération de modèle IA par persona : {} | prompt : {}", personaType, request.getUserPrompt());

        String systemPrompt = buildSystemPromptForPersona(personaType, request.getTargetLanguage());
        String fullUserPrompt = buildFullUserPrompt(personaType, request.getUserPrompt(), request.getTargetLanguage());

        AiClient client = aiClientManager.getActiveClient();
        String rawResponse = null;
        boolean fromMock = true;
        String provider = "MOCK_AI";

        if (aiClientManager.isAiEnabled() && aiClientManager.isGeminiAvailable()) {
            try {
                rawResponse = client.generateContent(systemPrompt, fullUserPrompt, true);
                fromMock = client instanceof MockAiClient;
                provider = client.getProviderName();
            } catch (Exception e) {
                log.warn("Échec génération Gemini ({}), bascule vers générateur intelligent par persona", e.getMessage());
            }
        }

        AiPersonaGenerationResponse response;
        if (rawResponse != null && !rawResponse.isBlank()) {
            try {
                response = parseAiResponse(rawResponse, personaType);
                response.setFromMock(fromMock);
                response.setProvider(provider);
            } catch (Exception e) {
                log.warn("Échec du parsing de la réponse LLM ({}), génération du modèle de secours sécurisé", e.getMessage());
                response = buildFallbackPersonaTemplate(personaType, request.getUserPrompt());
            }
        } else {
            response = buildFallbackPersonaTemplate(personaType, request.getUserPrompt());
        }

        // Enforce strict persona constraints (e.g. Canadian CV zero discrimination)
        enforcePersonaConstraints(response, personaType);
        response.setPromptUsed(request.getUserPrompt());

        return response;
    }

    private String normalizePersonaType(String personaType) {
        if (personaType == null) return "CV_CANADIAN";
        String upper = personaType.trim().toUpperCase();
        if (upper.contains("CANADIAN") || upper.contains("CANADA")) return "CV_CANADIAN";
        if (upper.contains("ATS") || upper.contains("US")) return "CV_ATS";
        if (upper.contains("EURO") || upper.contains("FRANCE")) return "CV_EUROPEEN";
        if (upper.contains("STAGE") || upper.contains("PFE") || upper.contains("ACADEMIC")) return "STAGE_REPORT";
        if (upper.contains("FACTUR") || upper.contains("INVOICE") || upper.contains("DEVIS")) return "FREELANCE_INVOICE";
        if (upper.contains("FLYER") || upper.contains("EVENT")) return "MARKETING_FLYER";
        return upper;
    }

    private String buildSystemPromptForPersona(String personaType, String lang) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tu es un expert mondial en génération de documents haute précision pour le persona : ").append(personaType).append(".\n");
        sb.append("Format attendu : Réponse JSON stricte sans balises markdown markdown supplémentaires.\n");

        switch (personaType) {
            case "CV_CANADIAN" -> {
                sb.append("RÈGLES IMPÉRATIVES NORMES CANADIENNES (Québec & Fédéral) :\n");
                sb.append("1. INTERDICTION FORMELLE DE METTRE UNE PHOTO, UN ÂGE, UNE DATE DE NAISSANCE, UNE SITUATION FAMILIALE, UN SEXE OU UNE NATIONALITÉ.\n");
                sb.append("2. Format de papier obligatoire : 'Letter' (216x279 mm).\n");
                sb.append("3. Les puces d'expériences DOIVENT commencer par des verbes d'action au passé et contenir des réalisations chiffrées (%, $, volumes).\n");
                sb.append("4. Sections obligatoires : Profil Sommaire, Compétences Clés, Expériences Professionnelles, Formations & Certifications.\n");
            }
            case "CV_ATS" -> {
                sb.append("RÈGLES IMPÉRATIVES ATS AMÉRICAIN (Applicant Tracking System) :\n");
                sb.append("1. Disposition 100% linéaire et épurée. ZÉRO photo, ZÉRO graphique SVG complexe, ZÉRO colonnes multiples.\n");
                sb.append("2. Format de papier : 'Letter' 1 page stricte.\n");
                sb.append("3. Titres de sections standards en majuscules (SUMMARY, SKILLS, EXPERIENCE, EDUCATION).\n");
            }
            case "STAGE_REPORT" -> {
                sb.append("RÈGLES IMPÉRATIVES RAPPORT DE STAGE / PFE ACADÉMIQUE :\n");
                sb.append("1. Format A4.\n");
                sb.append("2. Page de garde officielle complète avec logos, université, organisme d'accueil, tuteurs et promotion.\n");
                sb.append("3. Sommaire hiérarchique équilibré : Introduction, Présentation, Réalisations, Analyse critique, Remerciements, Conclusion.\n");
            }
            case "FREELANCE_INVOICE" -> {
                sb.append("RÈGLES IMPÉRATIVES FACTURE FREELANCE LÉGALE :\n");
                sb.append("1. Format A4 avec émetteur (SIRET, adresse), client, N° facture et date d'échéance.\n");
                sb.append("2. Tableau des prestations avec montants calculés, Total HT, TVA 20%, Total TTC.\n");
                sb.append("3. Coordonnées bancaires IBAN/BIC et mentions obligatoires d'indemnité de retard.\n");
            }
            default -> sb.append("Format de document soigné, professionnel et responsive.\n");
        }

        sb.append("Le JSON doit comporter les clés : 'nom', 'description', 'formatPapier', 'pages', 'schema', 'sampleData'.\n");
        return sb.toString();
    }

    private String buildFullUserPrompt(String personaType, String userPrompt, String lang) {
        return "Génère un modèle complet et des données adaptées pour : " + personaType + " en langue " + lang + " basé sur : " + userPrompt;
    }

    private AiPersonaGenerationResponse parseAiResponse(String rawResponse, String personaType) throws Exception {
        String cleanJson = rawResponse.trim();
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.substring(7);
        } else if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.substring(3);
        }
        if (cleanJson.endsWith("```")) {
            cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
        }
        cleanJson = cleanJson.trim();

        JsonNode root = objectMapper.readTree(cleanJson);
        String nom = root.path("nom").asText("Document " + personaType);
        String desc = root.path("description").asText("Généré par IA");
        String format = root.path("formatPapier").asText("Letter".equalsIgnoreCase(personaType) ? "Letter" : "A4");

        JsonNode pagesNode = root.has("pages") ? root.get("pages") : null;
        String contenuDesign = pagesNode != null ? objectMapper.writeValueAsString(Map.of("pages", pagesNode)) : "{\"pages\":[]}";

        String schema = root.has("schema") ? root.get("schema").toString() : "{\"type\":\"object\"}";

        Map<String, Object> sampleData = new HashMap<>();
        if (root.has("sampleData") && root.get("sampleData").isObject()) {
            sampleData = objectMapper.convertValue(root.get("sampleData"), Map.class);
        }

        return AiPersonaGenerationResponse.builder()
                .personaType(personaType)
                .nom(nom)
                .description(desc)
                .formatPapier(format)
                .contenuDesign(contenuDesign)
                .schema(schema)
                .sampleData(sampleData)
                .build();
    }

    public void enforcePersonaConstraints(AiPersonaGenerationResponse response, String personaType) {
        if ("CV_CANADIAN".equalsIgnoreCase(personaType)) {
            // Bannit explicitement photo, âge, situation maritale
            stripPhotoAndDiscriminatoryBlocks(response);
            response.setFormatPapier("Letter");
            response.setCategorie(Categorie.CV_CANADIAN.name());
        } else if ("CV_ATS".equalsIgnoreCase(personaType)) {
            stripPhotoAndDiscriminatoryBlocks(response);
            response.setFormatPapier("Letter");
            response.setCategorie(Categorie.CV_ATS.name());
        } else if ("STAGE_REPORT".equalsIgnoreCase(personaType)) {
            response.setFormatPapier("A4");
            response.setCategorie(Categorie.STAGE_ACADEMIQUE.name());
        } else if ("FREELANCE_INVOICE".equalsIgnoreCase(personaType)) {
            response.setFormatPapier("A4");
            response.setCategorie(Categorie.FACTURE_DEVIS.name());
        } else if ("MARKETING_FLYER".equalsIgnoreCase(personaType)) {
            response.setCategorie(Categorie.MARKETING_FLYER.name());
        }
    }

    private void stripPhotoAndDiscriminatoryBlocks(AiPersonaGenerationResponse response) {
        try {
            String design = response.getContenuDesign();
            if (design == null || design.isBlank()) return;

            JsonNode root = objectMapper.readTree(design);
            if (!root.has("pages") || !root.get("pages").isArray()) return;

            for (JsonNode page : root.get("pages")) {
                if (page.has("blocs") && page.get("blocs").isArray()) {
                    ArrayNode blocs = (ArrayNode) page.get("blocs");
                    List<JsonNode> preserved = new ArrayList<>();
                    for (JsonNode b : blocs) {
                        String type = b.path("type").asText("");
                        String id = b.path("id").asText("").toLowerCase();
                        String content = b.path("contenu").asText("").toLowerCase();

                        // Éliminer toute image de profil ou champs d'âge / état civil
                        boolean isPhoto = "image".equalsIgnoreCase(type) || id.contains("photo") || id.contains("avatar");
                        boolean isDiscriminatory = content.contains("marié") || content.contains("célibataire") ||
                                                  content.contains("âge") || content.contains("ans") && content.contains("permis") ||
                                                  content.contains("date de naissance") || content.contains("nationalité");

                        if (!isPhoto && !isDiscriminatory) {
                            preserved.add(b);
                        }
                    }
                    blocs.removeAll();
                    blocs.addAll(preserved);
                }
            }
            response.setContenuDesign(objectMapper.writeValueAsString(root));
        } catch (Exception e) {
            log.warn("Erreur lors du nettoyage anti-discrimination du CV Canadien/ATS : {}", e.getMessage());
        }
    }

    public AiPersonaGenerationResponse buildFallbackPersonaTemplate(String personaType, String userPrompt) {
        log.info("Construction du template persona intelligent de repli pour : {}", personaType);
        String promptLower = userPrompt != null ? userPrompt.toLowerCase() : "";

        return switch (personaType) {
            case "CV_CANADIAN" -> buildCanadianCvFallback(userPrompt);
            case "CV_ATS" -> buildAtsCvFallback(userPrompt);
            case "STAGE_REPORT" -> buildStageReportFallback(userPrompt);
            case "FREELANCE_INVOICE" -> buildInvoiceFallback(userPrompt);
            default -> buildCanadianCvFallback(userPrompt);
        };
    }

    private AiPersonaGenerationResponse buildCanadianCvFallback(String userPrompt) {
        String designJson = """
        {
          "pages": [
            {
              "nom": "Page 1",
              "blocs": [
                {
                  "id": "can-name",
                  "type": "titre",
                  "contenu": "{{nom_complet}}",
                  "x": 40,
                  "y": 40,
                  "largeurBox": 736,
                  "hauteurBox": 45,
                  "style": { "fontSize": 26, "fontWeight": "bold", "color": "#0f172a", "textAlign": "center" }
                },
                {
                  "id": "can-role",
                  "type": "texte",
                  "contenu": "{{titre_professionnel}}",
                  "x": 40,
                  "y": 88,
                  "largeurBox": 736,
                  "hauteurBox": 25,
                  "style": { "fontSize": 15, "fontWeight": "600", "color": "#0284c7", "textAlign": "center" }
                },
                {
                  "id": "can-contact",
                  "type": "texte",
                  "contenu": "{{ville_province}} • {{telephone}} • {{email}} • {{linkedin}}",
                  "x": 40,
                  "y": 115,
                  "largeurBox": 736,
                  "hauteurBox": 20,
                  "style": { "fontSize": 11, "color": "#475569", "textAlign": "center" }
                },
                {
                  "id": "can-sep-1",
                  "type": "ligne",
                  "x": 40,
                  "y": 140,
                  "largeurBox": 736,
                  "hauteurBox": 2,
                  "style": { "color": "#0284c7" }
                },
                {
                  "id": "can-sec-summary-t",
                  "type": "titre",
                  "contenu": "SOMMAIRE PROFESSIONNEL",
                  "x": 40,
                  "y": 155,
                  "largeurBox": 736,
                  "hauteurBox": 22,
                  "style": { "fontSize": 13, "fontWeight": "bold", "color": "#0f172a" }
                },
                {
                  "id": "can-sec-summary-c",
                  "type": "texte",
                  "contenu": "{{profil_sommaire}}",
                  "x": 40,
                  "y": 182,
                  "largeurBox": 736,
                  "hauteurBox": 60,
                  "style": { "fontSize": 11, "color": "#334155", "lineHeight": "1.5" }
                },
                {
                  "id": "can-sec-skills-t",
                  "type": "titre",
                  "contenu": "DOMAINES D'EXPERTISE & COMPÉTENCES CLÉS",
                  "x": 40,
                  "y": 250,
                  "largeurBox": 736,
                  "hauteurBox": 22,
                  "style": { "fontSize": 13, "fontWeight": "bold", "color": "#0f172a" }
                },
                {
                  "id": "can-sec-skills-c",
                  "type": "texte",
                  "contenu": "{{competences_cles}}",
                  "x": 40,
                  "y": 277,
                  "largeurBox": 736,
                  "hauteurBox": 55,
                  "style": { "fontSize": 11, "color": "#334155", "lineHeight": "1.5" }
                },
                {
                  "id": "can-sec-exp-t",
                  "type": "titre",
                  "contenu": "EXPÉRIENCE PROFESSIONNELLE & RÉALISATIONS CHIFFRÉES",
                  "x": 40,
                  "y": 340,
                  "largeurBox": 736,
                  "hauteurBox": 22,
                  "style": { "fontSize": 13, "fontWeight": "bold", "color": "#0f172a" }
                },
                {
                  "id": "can-exp-1",
                  "type": "texte",
                  "contenu": "<b>{{poste_actuel}}</b> | {{entreprise_actuelle}} ({{dates_actuel}})<br/>{{realisations_actuel}}",
                  "x": 40,
                  "y": 368,
                  "largeurBox": 736,
                  "hauteurBox": 120,
                  "style": { "fontSize": 11, "color": "#334155", "lineHeight": "1.5" }
                },
                {
                  "id": "can-sec-edu-t",
                  "type": "titre",
                  "contenu": "FORMATION & CERTIFICATIONS",
                  "x": 40,
                  "y": 500,
                  "largeurBox": 736,
                  "hauteurBox": 22,
                  "style": { "fontSize": 13, "fontWeight": "bold", "color": "#0f172a" }
                },
                {
                  "id": "can-sec-edu-c",
                  "type": "texte",
                  "contenu": "{{formations_texte}}",
                  "x": 40,
                  "y": 528,
                  "largeurBox": 736,
                  "hauteurBox": 80,
                  "style": { "fontSize": 11, "color": "#334155", "lineHeight": "1.5" }
                }
              ]
            }
          ]
        }
        """;

        String schemaJson = """
        {
          "type": "object",
          "properties": {
            "nom_complet": { "type": "string", "default": "Candidat IA Généré" },
            "titre_professionnel": { "type": "string", "default": "Expert Spécialiste & Consultant" },
            "ville_province": { "type": "string", "default": "Montréal, QC" },
            "telephone": { "type": "string", "default": "+1 (514) 789-0123" },
            "email": { "type": "string", "default": "candidat@quebec-emploi.ca" },
            "linkedin": { "type": "string", "default": "linkedin.com/in/profil-pro" },
            "profil_sommaire": { "type": "string", "default": "Professionnel d'impact spécialisé dans l'accélération des projets et l'optimisation des performances opérationnelles." },
            "competences_cles": { "type": "string", "default": "• Gestion de projets complexes • Analyse stratégique • Optimisation de processus" },
            "poste_actuel": { "type": "string", "default": "Consultant Principal" },
            "entreprise_actuelle": { "type": "string", "default": "Groupe Conseil Montréal" },
            "dates_actuel": { "type": "string", "default": "2021 - Présent" },
            "realisations_actuel": { "type": "string", "default": "• Amélioration de 35% de l'efficacité opérationnelle des flux documentaires.\\n• Réduction de 20% des coûts logistiques dès la première année." },
            "formations_texte": { "type": "string", "default": "• Baccalauréat Universitaire — Université de Montréal (2020)\\n• Certifications professionnelles de gestion" }
          }
        }
        """;

        Map<String, Object> sampleData = mapOf(
                "nom_complet", "Alexandre Martin",
                "titre_professionnel", "Spécialiste Informatique & DevOps",
                "ville_province", "Montréal, QC",
                "telephone", "+1 (514) 789-0123",
                "email", "alexandre.martin@emploi.ca",
                "linkedin", "linkedin.com/in/alex-martin",
                "profil_sommaire", "Ingénieur axé sur la livraison continue et la stabilité cloud.",
                "competences_cles", "• Cloud AWS / GCP • Docker • CI/CD GitLab",
                "poste_actuel", "Lead DevOps",
                "entreprise_actuelle", "Banque Laurentienne",
                "dates_actuel", "2022 - Présent",
                "realisations_actuel", "• Déploiement de clusters haute disponibilité (99.99%).\\n• Réduction de 40% des temps de livraison logicielle.",
                "formations_texte", "• Diplôme en Informatique — Polytechnique Montréal"
        );

        return AiPersonaGenerationResponse.builder()
                .personaType("CV_CANADIAN")
                .nom("CV Canadien Personnalisé (IA)")
                .description("Format officiel canadien généré par IA respectant les standards anti-discrimination.")
                .categorie(Categorie.CV_CANADIAN.name())
                .formatPapier("Letter")
                .contenuDesign(designJson)
                .schema(schemaJson)
                .sampleData(sampleData)
                .fromMock(true)
                .provider("AI_PERSONA_ENGINE")
                .build();
    }

    private AiPersonaGenerationResponse buildAtsCvFallback(String userPrompt) {
        String designJson = """
        {
          "pages": [
            {
              "nom": "Page 1",
              "blocs": [
                {
                  "id": "ats-name",
                  "type": "titre",
                  "contenu": "{{full_name}}",
                  "x": 35,
                  "y": 35,
                  "largeurBox": 746,
                  "hauteurBox": 35,
                  "style": { "fontSize": 24, "fontWeight": "bold", "color": "#000000", "textAlign": "center" }
                },
                {
                  "id": "ats-contact",
                  "type": "texte",
                  "contenu": "{{phone}} | {{email}} | {{linkedin}} | {{city_state}}",
                  "x": 35,
                  "y": 75,
                  "largeurBox": 746,
                  "hauteurBox": 20,
                  "style": { "fontSize": 10, "color": "#111111", "textAlign": "center" }
                },
                {
                  "id": "ats-summary-t",
                  "type": "titre",
                  "contenu": "EXECUTIVE SUMMARY",
                  "x": 35,
                  "y": 105,
                  "largeurBox": 746,
                  "hauteurBox": 20,
                  "style": { "fontSize": 12, "fontWeight": "bold", "color": "#000000" }
                },
                {
                  "id": "ats-summary-c",
                  "type": "texte",
                  "contenu": "{{summary}}",
                  "x": 35,
                  "y": 128,
                  "largeurBox": 746,
                  "hauteurBox": 55,
                  "style": { "fontSize": 10, "color": "#111111", "lineHeight": "1.4" }
                },
                {
                  "id": "ats-skills-t",
                  "type": "titre",
                  "contenu": "CORE COMPETENCIES & TECHNICAL SKILLS",
                  "x": 35,
                  "y": 190,
                  "largeurBox": 746,
                  "hauteurBox": 20,
                  "style": { "fontSize": 12, "fontWeight": "bold", "color": "#000000" }
                },
                {
                  "id": "ats-skills-c",
                  "type": "texte",
                  "contenu": "{{skills}}",
                  "x": 35,
                  "y": 212,
                  "largeurBox": 746,
                  "hauteurBox": 50,
                  "style": { "fontSize": 10, "color": "#111111", "lineHeight": "1.4" }
                },
                {
                  "id": "ats-exp-t",
                  "type": "titre",
                  "contenu": "PROFESSIONAL EXPERIENCE",
                  "x": 35,
                  "y": 270,
                  "largeurBox": 746,
                  "hauteurBox": 20,
                  "style": { "fontSize": 12, "fontWeight": "bold", "color": "#000000" }
                },
                {
                  "id": "ats-exp-c",
                  "type": "texte",
                  "contenu": "<b>{{recent_title}}</b> — {{recent_company}}, {{recent_location}} ({{recent_dates}})<br/>{{recent_achievements}}",
                  "x": 35,
                  "y": 294,
                  "largeurBox": 746,
                  "hauteurBox": 140,
                  "style": { "fontSize": 10, "color": "#111111", "lineHeight": "1.4" }
                },
                {
                  "id": "ats-edu-t",
                  "type": "titre",
                  "contenu": "EDUCATION & CERTIFICATIONS",
                  "x": 35,
                  "y": 445,
                  "largeurBox": 746,
                  "hauteurBox": 20,
                  "style": { "fontSize": 12, "fontWeight": "bold", "color": "#000000" }
                },
                {
                  "id": "ats-edu-c",
                  "type": "texte",
                  "contenu": "{{education}}",
                  "x": 35,
                  "y": 468,
                  "largeurBox": 746,
                  "hauteurBox": 60,
                  "style": { "fontSize": 10, "color": "#111111", "lineHeight": "1.4" }
                }
              ]
            }
          ]
        }
        """;

        String schemaJson = """
        {
          "type": "object",
          "properties": {
            "full_name": { "type": "string", "default": "David Miller" },
            "phone": { "type": "string", "default": "+1 (212) 555-0143" },
            "email": { "type": "string", "default": "david.miller@techcareer.com" },
            "linkedin": { "type": "string", "default": "linkedin.com/in/dmiller" },
            "city_state": { "type": "string", "default": "New York, NY" },
            "summary": { "type": "string", "default": "Strategic leader with 8+ years experience architecting scalable backend systems." },
            "skills": { "type": "string", "default": "Java, Spring Boot, AWS, Docker, Kubernetes, SQL, CI/CD" },
            "recent_title": { "type": "string", "default": "Principal Systems Engineer" },
            "recent_company": { "type": "string", "default": "FinTech Capital NY" },
            "recent_location": { "type": "string", "default": "New York, NY" },
            "recent_dates": { "type": "string", "default": "2021 - Present" },
            "recent_achievements": { "type": "string", "default": "• Engineered microservices infrastructure servicing 3M queries per second.\\n• Reduced API latency from 120ms to 24ms." },
            "education": { "type": "string", "default": "B.S. in Computer Science — Columbia University" }
          }
        }
        """;

        Map<String, Object> sampleData = mapOf(
                "full_name", "David Miller",
                "phone", "+1 (212) 555-0143",
                "email", "david.miller@techcareer.com",
                "linkedin", "linkedin.com/in/dmiller",
                "city_state", "New York, NY",
                "summary", "Strategic leader with 8+ years experience architecting scalable backend systems.",
                "skills", "Java, Spring Boot, AWS, Docker, Kubernetes, SQL, CI/CD",
                "recent_title", "Principal Systems Engineer",
                "recent_company", "FinTech Capital NY",
                "recent_location", "New York, NY",
                "recent_dates", "2021 - Present",
                "recent_achievements", "• Engineered microservices infrastructure servicing 3M queries per second.\\n• Reduced API latency from 120ms to 24ms.",
                "education", "B.S. in Computer Science — Columbia University"
        );

        return AiPersonaGenerationResponse.builder()
                .personaType("CV_ATS")
                .nom("CV US ATS 1-Page (IA)")
                .description("Format strict US Letter 1-page sans aucun élément bloquant pour les robots recruteurs.")
                .categorie(Categorie.CV_ATS.name())
                .formatPapier("Letter")
                .contenuDesign(designJson)
                .schema(schemaJson)
                .sampleData(sampleData)
                .fromMock(true)
                .provider("AI_PERSONA_ENGINE")
                .build();
    }

    private AiPersonaGenerationResponse buildStageReportFallback(String userPrompt) {
        String designJson = """
        {
          "pages": [
            {
              "nom": "Page de Garde",
              "blocs": [
                {
                  "id": "stg-univ",
                  "type": "texte",
                  "contenu": "<b>{{universite}}</b> — {{faculte}}",
                  "x": 40,
                  "y": 45,
                  "largeurBox": 714,
                  "hauteurBox": 30,
                  "style": { "fontSize": 14, "color": "#475569", "textAlign": "center" }
                },
                {
                  "id": "stg-titre-doc",
                  "type": "texte",
                  "contenu": "MÉMOIRE & RAPPORT DE STAGE DE FIN D'ÉTUDES",
                  "x": 40,
                  "y": 180,
                  "largeurBox": 714,
                  "hauteurBox": 30,
                  "style": { "fontSize": 16, "letterSpacing": "2px", "fontWeight": "bold", "color": "#0284c7", "textAlign": "center" }
                },
                {
                  "id": "stg-sujet",
                  "type": "titre",
                  "contenu": "{{titre_sujet}}",
                  "x": 40,
                  "y": 230,
                  "largeurBox": 714,
                  "hauteurBox": 65,
                  "style": { "fontSize": 24, "fontWeight": "800", "color": "#0f172a", "textAlign": "center" }
                },
                {
                  "id": "stg-box",
                  "type": "rectangle",
                  "x": 60,
                  "y": 410,
                  "largeurBox": 674,
                  "hauteurBox": 240,
                  "style": { "backgroundColor": "#f8fafc", "border": "1px solid #e2e8f0" }
                },
                {
                  "id": "stg-etudiant",
                  "type": "texte",
                  "contenu": "<b>Présenté par :</b><br/>{{nom_etudiant}}<br/>Filière : {{filiere_promo}}",
                  "x": 90,
                  "y": 435,
                  "largeurBox": 280,
                  "hauteurBox": 70,
                  "style": { "fontSize": 12, "color": "#1e293b", "lineHeight": "1.5" }
                },
                {
                  "id": "stg-ent",
                  "type": "texte",
                  "contenu": "<b>Organisme d'accueil :</b><br/>{{entreprise_accueil}}<br/>Lieu : {{lieu_stage}}",
                  "x": 410,
                  "y": 435,
                  "largeurBox": 280,
                  "hauteurBox": 70,
                  "style": { "fontSize": 12, "color": "#1e293b", "lineHeight": "1.5" }
                },
                {
                  "id": "stg-tuteur-acad",
                  "type": "texte",
                  "contenu": "<b>Tuteur académique :</b><br/>{{tuteur_universitaire}}",
                  "x": 90,
                  "y": 525,
                  "largeurBox": 280,
                  "hauteurBox": 60,
                  "style": { "fontSize": 12, "color": "#1e293b", "lineHeight": "1.5" }
                },
                {
                  "id": "stg-tuteur-pro",
                  "type": "texte",
                  "contenu": "<b>Maître de stage :</b><br/>{{maitre_stage}}",
                  "x": 410,
                  "y": 525,
                  "largeurBox": 280,
                  "hauteurBox": 60,
                  "style": { "fontSize": 12, "color": "#1e293b", "lineHeight": "1.5" }
                },
                {
                  "id": "stg-annee",
                  "type": "texte",
                  "contenu": "Année Académique {{annee_universitaire}}",
                  "x": 40,
                  "y": 760,
                  "largeurBox": 714,
                  "hauteurBox": 25,
                  "style": { "fontSize": 12, "fontWeight": "600", "color": "#64748b", "textAlign": "center" }
                }
              ]
            }
          ]
        }
        """;

        String schemaJson = """
        {
          "type": "object",
          "properties": {
            "universite": { "type": "string", "default": "Sorbonne Université" },
            "faculte": { "type": "string", "default": "Faculté des Sciences & Ingénierie" },
            "titre_sujet": { "type": "string", "default": "Automatisation et Déploiement Continu de Plateformes Cloud" },
            "nom_etudiant": { "type": "string", "default": "Camille Leroy" },
            "filiere_promo": { "type": "string", "default": "Master 2 Informatique & Cloud" },
            "entreprise_accueil": { "type": "string", "default": "Tech Innovation SAS" },
            "lieu_stage": { "type": "string", "default": "Paris, France" },
            "tuteur_universitaire": { "type": "string", "default": "Pr. Antoine Dubois" },
            "maitre_stage": { "type": "string", "default": "Mme. Valérie Moreau" },
            "annee_universitaire": { "type": "string", "default": "2025 - 2026" }
          }
        }
        """;

        Map<String, Object> sampleData = mapOf(
                "universite", "Sorbonne Université",
                "faculte", "Faculté des Sciences & Ingénierie",
                "titre_sujet", "Automatisation et Déploiement Continu de Plateformes Cloud",
                "nom_etudiant", "Camille Leroy",
                "filiere_promo", "Master 2 Informatique & Cloud",
                "entreprise_accueil", "Tech Innovation SAS",
                "lieu_stage", "Paris, France",
                "tuteur_universitaire", "Pr. Antoine Dubois",
                "maitre_stage", "Mme. Valérie Moreau",
                "annee_universitaire", "2025 - 2026"
        );

        return AiPersonaGenerationResponse.builder()
                .personaType("STAGE_REPORT")
                .nom("Rapport Académique & PFE (IA)")
                .description("Page de garde et structure académique conforme aux universités françaises et européennes.")
                .categorie(Categorie.STAGE_ACADEMIQUE.name())
                .formatPapier("A4")
                .contenuDesign(designJson)
                .schema(schemaJson)
                .sampleData(sampleData)
                .fromMock(true)
                .provider("AI_PERSONA_ENGINE")
                .build();
    }

    private AiPersonaGenerationResponse buildInvoiceFallback(String userPrompt) {
        String designJson = """
        {
          "pages": [
            {
              "nom": "Page 1",
              "blocs": [
                {
                  "id": "inv-emetteur",
                  "type": "texte",
                  "contenu": "<b>{{emetteur_nom}}</b><br/>SIRET : {{emetteur_siret}}<br/>{{emetteur_adresse}}<br/>{{emetteur_email}}",
                  "x": 40,
                  "y": 40,
                  "largeurBox": 320,
                  "hauteurBox": 80,
                  "style": { "fontSize": 11, "color": "#1e293b", "lineHeight": "1.4" }
                },
                {
                  "id": "inv-title",
                  "type": "titre",
                  "contenu": "FACTURE",
                  "x": 450,
                  "y": 35,
                  "largeurBox": 304,
                  "hauteurBox": 35,
                  "style": { "fontSize": 26, "fontWeight": "bold", "color": "#0284c7", "textAlign": "right" }
                },
                {
                  "id": "inv-dates",
                  "type": "texte",
                  "contenu": "N° Facture : <b>{{numero_facture}}</b><br/>Date : {{date_facture}}<br/>Échéance : {{date_echeance}}",
                  "x": 450,
                  "y": 75,
                  "largeurBox": 304,
                  "hauteurBox": 55,
                  "style": { "fontSize": 11, "color": "#475569", "textAlign": "right", "lineHeight": "1.5" }
                },
                {
                  "id": "inv-table",
                  "type": "tableau",
                  "source": "{{lignes}}",
                  "x": 40,
                  "y": 240,
                  "largeurBox": 714,
                  "hauteurBox": 150,
                  "style": { "headerBackground": "#0f172a", "headerColor": "#ffffff", "borderColor": "#e2e8f0" }
                },
                {
                  "id": "inv-totals",
                  "type": "texte",
                  "contenu": "Total HT : <b>{{total_ht}} €</b><br/>TVA (20%) : <b>{{total_tva}} €</b><br/><span style='font-size:14px;color:#0284c7;'>Total TTC : <b>{{total_ttc}} €</b></span>",
                  "x": 480,
                  "y": 420,
                  "largeurBox": 274,
                  "hauteurBox": 75,
                  "style": { "fontSize": 12, "color": "#0f172a", "textAlign": "right", "lineHeight": "1.7" }
                },
                {
                  "id": "inv-bank",
                  "type": "texte",
                  "contenu": "<b>Règlement par virement :</b><br/>IBAN : {{iban}}<br/>BIC : {{bic}}",
                  "x": 40,
                  "y": 420,
                  "largeurBox": 380,
                  "hauteurBox": 60,
                  "style": { "fontSize": 11, "color": "#334155", "lineHeight": "1.5" }
                }
              ]
            }
          ]
        }
        """;

        String schemaJson = """
        {
          "type": "object",
          "properties": {
            "emetteur_nom": { "type": "string", "default": "Freelance Solutions" },
            "emetteur_siret": { "type": "string", "default": "892 411 902 00018" },
            "emetteur_adresse": { "type": "string", "default": "10 Rue de la Paix, 75002 Paris" },
            "emetteur_email": { "type": "string", "default": "contact@freelance.fr" },
            "numero_facture": { "type": "string", "default": "FA-2026-001" },
            "date_facture": { "type": "string", "default": "15/10/2026" },
            "date_echeance": { "type": "string", "default": "15/11/2026" },
            "total_ht": { "type": "string", "default": "3 200,00" },
            "total_tva": { "type": "string", "default": "640,00" },
            "total_ttc": { "type": "string", "default": "3 840,00" },
            "iban": { "type": "string", "default": "FR76 3000 4000 0100 1234 5678 901" },
            "bic": { "type": "string", "default": "BNPAFRPPXXX" }
          }
        }
        """;

        Map<String, Object> sampleData = mapOf(
                "emetteur_nom", "Freelance Solutions",
                "emetteur_siret", "892 411 902 00018",
                "emetteur_adresse", "10 Rue de la Paix, 75002 Paris",
                "emetteur_email", "contact@freelance.fr",
                "numero_facture", "FA-2026-001",
                "date_facture", "15/10/2026",
                "date_echeance", "15/11/2026",
                "total_ht", "3 200,00",
                "total_tva", "640,00",
                "total_ttc", "3 840,00",
                "iban", "FR76 3000 4000 0100 1234 5678 901",
                "bic", "BNPAFRPPXXX"
        );

        return AiPersonaGenerationResponse.builder()
                .personaType("FREELANCE_INVOICE")
                .nom("Facture Freelance Conforme (IA)")
                .description("Facture pour travailleur indépendant avec ventilation de TVA et mentions légales.")
                .categorie(Categorie.FACTURE_DEVIS.name())
                .formatPapier("A4")
                .contenuDesign(designJson)
                .schema(schemaJson)
                .sampleData(sampleData)
                .fromMock(true)
                .provider("AI_PERSONA_ENGINE")
                .build();
    }

    private Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }
}

