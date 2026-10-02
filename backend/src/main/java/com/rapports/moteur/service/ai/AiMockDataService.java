package com.rapports.moteur.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.dtoAi.AiMockDataRequest;
import com.rapports.moteur.dto.dtoAi.AiMockDataResponse;
import com.rapports.moteur.dto.dtoVariable.ExtractedVariable;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.exceptions.TemplateNotFoundException;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.service.EntrepriseService;
import com.rapports.moteur.service.SchemaExtractorService;
import com.rapports.moteur.service.audit.AuditTrailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiMockDataService {

    private final ReportTemplateRepository templateRepository;
    private final SchemaExtractorService schemaExtractorService;
    private final EntrepriseService entrepriseService;
    private final AuditTrailService auditTrailService;
    private final AiClientManager aiClientManager;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_INSTRUCTION = """
        Tu es un générateur expert de jeux de données de test professionnels, réalistes et cohérents pour des documents d'entreprise français (devis, factures, fiches de paie, rapports).

        Génère un objet JSON unique contenant des valeurs pour chacune des variables demandées.
        Règles strictes :
        1. Les valeurs doivent être réalistes pour le contexte français (vrais noms de sociétés, adresses réelles avec codes postaux, SIRET 14 chiffres valides, montants plausibles).
        2. Cohérence mathématique : si des variables de totaux, TVA, ou lignes d'articles sont présentes :
           - Total HT = Somme des (quantite * prix_unitaire) de chaque ligne
           - TVA = Total HT * taux_tva (ex: 20%)
           - Total TTC = Total HT + TVA
        3. Pour les tableaux dynamiques / listes (clé demandée ou "lignes") :
           - Fournis un tableau JSON d'objets avec les colonnes spécifiées.
        4. Réponds STRICTEMENT en format JSON valide, sans balises markdown, sans texte additionnel.
        """;

    public AiMockDataResponse generateMockData(AiMockDataRequest request) {
        List<AiMockDataRequest.VariableItem> variables = new ArrayList<>();
        Map<String, List<String>> arrayColumnsMap = new LinkedHashMap<>();
        String templateNom = request.getTemplateNom();

        // Si un templateId est fourni, charger les variables depuis le template
        if (request.getTemplateId() != null) {
            ReportTemplate template = loadTemplateForCurrentEntreprise(request.getTemplateId());
            if (templateNom == null || templateNom.isBlank()) {
                templateNom = template.getNom();
            }

            if (request.getVariables() == null || request.getVariables().isEmpty()) {
                List<ExtractedVariable> extracted = schemaExtractorService.extract(template.getContenuDesign());
                for (ExtractedVariable ev : extracted) {
                    variables.add(new AiMockDataRequest.VariableItem(ev.getNom(), ev.getType(), null));
                }
            } else {
                variables.addAll(request.getVariables());
            }

            arrayColumnsMap = schemaExtractorService.extractArrayColumns(template.getContenuDesign());
        } else if (request.getVariables() != null) {
            variables.addAll(request.getVariables());
        }

        int rowCount = request.getRowCount() != null && request.getRowCount() > 0 ? request.getRowCount() : 3;

        // Construire le prompt contextuel
        String userPrompt = buildPrompt(templateNom, variables, arrayColumnsMap, request.getArrayColumns(), rowCount);

        AtomicBoolean usedMock = new AtomicBoolean(false);
        String rawJson = aiClientManager.executeWithFallback(SYSTEM_INSTRUCTION, userPrompt, true, usedMock);

        Map<String, Object> dataMap;
        try {
            String cleaned = sanitizeJson(rawJson);
            dataMap = objectMapper.readValue(cleaned, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Erreur de désérialisation du JSON de mock data IA ({}), génération d'un jeu de secours...", e.getMessage());
            dataMap = generateHeuristicFallbackData(variables, arrayColumnsMap, rowCount);
            usedMock.set(true);
        }

        // Piste d'audit
        auditTrailService.log(
                "AI_MOCK_DATA_GENERATE",
                "MockData",
                request.getTemplateId() != null ? request.getTemplateId().toString() : "adhoc",
                String.format("{\"variablesCount\":%d,\"fromMock\":%b}", variables.size(), usedMock.get()),
                "SUCCESS"
        );

        return AiMockDataResponse.builder()
                .data(dataMap)
                .fromAi(!usedMock.get())
                .provider(usedMock.get() ? "mock" : aiClientManager.getCurrentProvider())
                .message("Données de test générées avec succès")
                .build();
    }

    private String buildPrompt(String templateNom,
                               List<AiMockDataRequest.VariableItem> variables,
                               Map<String, List<String>> arrayColumnsMap,
                               List<String> explicitArrayCols,
                               int rowCount) {
        StringBuilder sb = new StringBuilder();
        sb.append("Génère un jeu de données de test JSON réaliste pour le document suivant : \n");
        if (templateNom != null && !templateNom.isBlank()) {
            sb.append("Titre / Contexte du document : ").append(templateNom).append("\n");
        }

        sb.append("\nVariables scalaires attendues :\n");
        for (AiMockDataRequest.VariableItem v : variables) {
            sb.append("- ").append(v.getNom()).append(" (type: ").append(v.getType() != null ? v.getType() : "STRING").append(")");
            if (v.getDescription() != null && !v.getDescription().isBlank()) {
                sb.append(" : ").append(v.getDescription());
            }
            sb.append("\n");
        }

        if (!arrayColumnsMap.isEmpty()) {
            sb.append("\nTableaux dynamiques attendus (générer ").append(rowCount).append(" lignes chacun) :\n");
            for (Map.Entry<String, List<String>> entry : arrayColumnsMap.entrySet()) {
                sb.append("- Tableau '").append(entry.getKey()).append("' avec colonnes : ")
                        .append(String.join(", ", entry.getValue())).append("\n");
            }
        } else if (explicitArrayCols != null && !explicitArrayCols.isEmpty()) {
            sb.append("\nTableau dynamique 'lignes' (générer ").append(rowCount).append(" lignes) avec colonnes : ")
                    .append(String.join(", ", explicitArrayCols)).append("\n");
        }

        return sb.toString();
    }

    private Map<String, Object> generateHeuristicFallbackData(
            List<AiMockDataRequest.VariableItem> variables,
            Map<String, List<String>> arrayColumnsMap,
            int rowCount) {

        Map<String, Object> map = new LinkedHashMap<>();
        for (AiMockDataRequest.VariableItem v : variables) {
            String name = v.getNom();
            String lower = name.toLowerCase();

            if (lower.contains("date")) {
                map.put(name, LocalDate.now().toString());
            } else if (lower.contains("email")) {
                map.put(name, "contact@entreprise-exemple.fr");
            } else if (lower.contains("siret")) {
                map.put(name, "732 829 320 00074");
            } else if (lower.contains("tel") || lower.contains("phone")) {
                map.put(name, "01 42 68 55 00");
            } else if (lower.contains("adresse")) {
                map.put(name, "18 Boulevard Haussmann, 75009 Paris");
            } else if (lower.contains("client") || lower.contains("destinataire")) {
                map.put(name, "Société Durand & Associés");
            } else if (lower.contains("entreprise") || lower.contains("societe") || lower.contains("emetteur")) {
                map.put(name, "Solutions Métier Tech SAS");
            } else if (lower.contains("iban")) {
                map.put(name, "FR76 3000 4000 1234 5678 9012 345");
            } else if (lower.contains("bic")) {
                map.put(name, "BNPAFRPP");
            } else if (lower.contains("total_ht")) {
                map.put(name, 2500.00);
            } else if (lower.contains("tva") || lower.contains("montant_tva")) {
                map.put(name, 500.00);
            } else if (lower.contains("total") || lower.contains("total_ttc")) {
                map.put(name, 3000.00);
            } else if ("NUMBER".equalsIgnoreCase(v.getType()) || "FLOAT".equalsIgnoreCase(v.getType())) {
                map.put(name, 100.0);
            } else if ("BOOLEAN".equalsIgnoreCase(v.getType())) {
                map.put(name, true);
            } else {
                map.put(name, "Valeur " + name);
            }
        }

        // Tableaux dynamiques
        if (!arrayColumnsMap.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : arrayColumnsMap.entrySet()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                for (int i = 1; i <= rowCount; i++) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (String col : entry.getValue()) {
                        String colLower = col.toLowerCase();
                        if (colLower.contains("quant") || colLower.contains("qte")) {
                            row.put(col, i);
                        } else if (colLower.contains("prix") || colLower.contains("pu")) {
                            row.put(col, 150.00 * i);
                        } else if (colLower.contains("total")) {
                            row.put(col, 150.00 * i * i);
                        } else {
                            row.put(col, "Prestation n°" + i);
                        }
                    }
                    rows.add(row);
                }
                map.put(entry.getKey(), rows);
            }
        }

        return map;
    }

    private ReportTemplate loadTemplateForCurrentEntreprise(UUID id) {
        ReportTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + id));
        String currentCode = entrepriseService.getCurrentCodeEntreprise();
        if (template.getCodeEntreprise() != null && !template.getCodeEntreprise().isBlank()) {
            if (currentCode == null || !currentCode.equals(template.getCodeEntreprise())) {
                throw new TemplateNotFoundException("Template introuvable : " + id);
            }
        }
        return template;
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
}
