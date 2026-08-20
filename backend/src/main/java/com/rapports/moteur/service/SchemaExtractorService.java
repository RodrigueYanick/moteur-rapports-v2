package com.rapports.moteur.service;

import com.fasterxml.jackson.databind.*;
import com.rapports.moteur.dto.dtoVariable.ExtractedVariable;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.*;

@Service
public class SchemaExtractorService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(.+?)\\}\\}");


    public List<ExtractedVariable> extract(String contenuDesignJson) {
        Map<String, String> varTypes = new LinkedHashMap<>();
        if (contenuDesignJson == null || contenuDesignJson.isBlank()) return Collections.emptyList();

        try {
            JsonNode root = objectMapper.readTree(contenuDesignJson);
            List<JsonNode> allBlocs = collectAllBlocs(root);
            for (JsonNode bloc : allBlocs) {
                processBloc(bloc, varTypes);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Design JSON invalide : " + e.getMessage(), e);
        }

        List<ExtractedVariable> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : varTypes.entrySet()) {
            result.add(new ExtractedVariable(entry.getKey(), entry.getValue(), true));
        }
        return result;
    }

    /**
     * Extrait, pour chaque tableau dynamique du design, la liste des colonnes attendues
     * (nom de variable -> ne couvre pas les colonnes calculées via "formule", volontairement,
     * puisque ces colonnes ne sont jamais fournies par l'ERP, elles sont dérivées).
     *
     * @return Map<nomVariableARRAY, List<nomColonneAttendue>>
     */
    public Map<String, List<String>> extractArrayColumns(String contenuDesignJson) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        if (contenuDesignJson == null || contenuDesignJson.isBlank()) return result;

        try {
            JsonNode root = objectMapper.readTree(contenuDesignJson);
            List<JsonNode> allBlocs = collectAllBlocs(root);

            for (JsonNode bloc : allBlocs) {
                if (!"tableau".equals(bloc.path("type").asText())) continue;
                if (bloc.has("lignes")) continue; // tableau statique, pas concerné

                String source = stripBraces(bloc.path("source").asText(""));
                if (source.isBlank()) continue;

                List<String> columnNames = new ArrayList<>();
                for (JsonNode col : bloc.path("colonnes")) {
                    // Une colonne avec formule est calculée, jamais fournie par l'ERP -> exclue de la validation
                    boolean hasFormula = col.has("formule") && !col.path("formule").asText("").isBlank();
                    if (hasFormula) continue;
                    String varName = col.path("variable").asText("");
                    if (!varName.isBlank()) columnNames.add(varName);
                }
                result.put(source, columnNames);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Design JSON invalide (colonnes) : " + e.getMessage(), e);
        }
        return result;
    }

    /** Rassemble tous les blocs, qu'ils soient sous "pages[].blocs" ou l'ancien format plat "blocs". */
    private List<JsonNode> collectAllBlocs(JsonNode root) {
        List<JsonNode> allBlocs = new ArrayList<>();
        if (root.has("pages") && root.path("pages").isArray()) {
            for (JsonNode page : root.path("pages")) {
                for (JsonNode bloc : page.path("blocs")) allBlocs.add(bloc);
            }
        } else if (root.has("blocs") && root.path("blocs").isArray()) {
            for (JsonNode bloc : root.path("blocs")) allBlocs.add(bloc);
        }
        return allBlocs;
    }

    private void processBloc(JsonNode bloc, Map<String, String> varTypes) {
        String type = bloc.path("type").asText();

        if (bloc.has("contenu")) {
            String contenu = bloc.path("contenu").asText();
            Matcher matcher = VAR_PATTERN.matcher(contenu);
            while (matcher.find()) {
                String varName = matcher.group(1).trim();
                varTypes.putIfAbsent(varName, determineType(varName));
            }
        }

        if ("image".equals(type) && bloc.has("url")) {
            String url = bloc.path("url").asText();
            Matcher matcher = VAR_PATTERN.matcher(url);
            while (matcher.find()) {
                String varName = matcher.group(1).trim();
                varTypes.putIfAbsent(varName, "STRING");
            }
        }

        if ("tableau".equals(type) && bloc.has("source") && !bloc.has("lignes")) {
            String source = stripBraces(bloc.path("source").asText(""));
            if (!source.isBlank()) varTypes.put(source, "ARRAY");
        }
    }

    private String stripBraces(String source) {
        return source.replace("{{", "").replace("}}", "").trim();
    }

    private String determineType(String varName) {
        String lower = varName.toLowerCase();
        if (lower.contains("date")) return "DATE";
        if (lower.contains("montant") || lower.contains("prix") || lower.contains("total") || lower.contains("tva")) return "FLOAT";
        if (lower.startsWith("est") || lower.contains("valide")) return "BOOLEAN";
        return "STRING";
    }
}