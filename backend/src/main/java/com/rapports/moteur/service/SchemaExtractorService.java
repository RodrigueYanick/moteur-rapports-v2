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
            JsonNode blocs = root.path("blocs");
            if (!blocs.isArray()) return Collections.emptyList();

            for (JsonNode bloc : blocs) {
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

    private void processBloc(JsonNode bloc, Map<String, String> varTypes) {
        String type = bloc.path("type").asText();

        // Variables dans le champ "contenu"
        if (bloc.has("contenu")) {
            String contenu = bloc.path("contenu").asText();
            Matcher matcher = VAR_PATTERN.matcher(contenu);
            while (matcher.find()) {
                String varName = matcher.group(1).trim();
                varTypes.putIfAbsent(varName, determineType(varName));
            }
        }

        // Variables dans l'URL des blocs image
        if ("image".equals(type) && bloc.has("url")) {
            String url = bloc.path("url").asText();
            Matcher matcher = VAR_PATTERN.matcher(url);
            while (matcher.find()) {
                String varName = matcher.group(1).trim();
                varTypes.putIfAbsent(varName, "STRING");
            }
        }

        // Pour un tableau, la "source" est une variable de type ARRAY
        if ("tableau".equals(type) && bloc.has("source")) {
            String source = bloc.path("source").asText().trim();
            source = source.replace("{{", "").replace("}}", "").trim();
            varTypes.put(source, "ARRAY");
        }

        // Variables dans le champ "url" des blocs image
        if ("image".equals(type) && bloc.has("url")) {
            String url = bloc.path("url").asText();
            Matcher matcher = VAR_PATTERN.matcher(url);
            while (matcher.find()) {
                String varName = matcher.group(1).trim();
                varTypes.putIfAbsent(varName, "STRING");
            }
        }
    }

    private String determineType(String varName) {
        String lower = varName.toLowerCase();
        if (lower.contains("date")) return "DATE";
        if (lower.contains("montant") || lower.contains("prix") || lower.contains("total") || lower.contains("tva")) return "FLOAT";
        if (lower.startsWith("est") || lower.contains("valide")) return "BOOLEAN";
        return "STRING";
    }
}