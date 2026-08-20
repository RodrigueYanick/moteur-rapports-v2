package com.rapports.moteur.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.exceptions.ValidationException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DataValidatorService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SchemaExtractorService schemaExtractor;

    public DataValidatorService(SchemaExtractorService schemaExtractor) {
        this.schemaExtractor = schemaExtractor;
    }

    /**
     * Valide les données par rapport au schéma JSON stocké dans le template,
     * PUIS valide la structure interne de chaque variable ARRAY par rapport
     * aux colonnes définies dans le design (contenuDesign).
     */
    public void validate(String schemaJson, Map<String, Object> data) {
        validate(schemaJson, data, null);
    }

    public void validate(String schemaJson, Map<String, Object> data, String contenuDesignJson) {
        List<String> errors = new ArrayList<>();

        if (schemaJson != null && !schemaJson.isBlank()) {
            errors.addAll(validateAgainstSchema(schemaJson, data));
        }

        if (contenuDesignJson != null && !contenuDesignJson.isBlank()) {
            errors.addAll(validateArrayStructures(contenuDesignJson, data));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private List<String> validateAgainstSchema(String schemaJson, Map<String, Object> data) {
        List<String> errors = new ArrayList<>();
        try {
            JsonNode array = objectMapper.readTree(schemaJson);
            if (!array.isArray()) return errors;

            for (JsonNode node : array) {
                String nom = node.path("nomVariable").asText();
                boolean obligatoire = node.path("obligatoire").asBoolean();
                String typeStr = node.path("type").asText();

                if ("CALCULEE".equals(typeStr)) continue; // jamais fourni par l'utilisateur

                Object value = data.get(nom);

                if (obligatoire && value == null) {
                    errors.add("Champ obligatoire manquant : " + nom);
                    continue;
                }
                if (value == null) continue;

                if (!isTypeValid(typeStr, value)) {
                    errors.add("Type invalide pour '" + nom + "' : attendu " + typeStr);
                }
            }
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lecture schéma JSON", e);
        }
        return errors;
    }

    /**
     * Vérifie que chaque ligne de chaque variable ARRAY contient bien toutes
     * les colonnes attendues (définies dans le design du tableau dynamique),
     * avec un type minimalement cohérent.
     */
    @SuppressWarnings("unchecked")
    private List<String> validateArrayStructures(String contenuDesignJson, Map<String, Object> data) {
        List<String> errors = new ArrayList<>();
        Map<String, List<String>> arrayColumns;
        try {
            arrayColumns = schemaExtractor.extractArrayColumns(contenuDesignJson);
        } catch (Exception e) {
            // Design illisible : on ne bloque pas la génération pour autant, on ignore cette passe
            return errors;
        }

        for (Map.Entry<String, List<String>> entry : arrayColumns.entrySet()) {
            String arrayVarName = entry.getKey();
            List<String> expectedColumns = entry.getValue();
            if (expectedColumns.isEmpty()) continue;

            Object arrayValue = data.get(arrayVarName);
            if (arrayValue == null) continue; // déjà signalé par validateAgainstSchema si obligatoire

            if (!(arrayValue instanceof List<?> rows)) {
                errors.add("La variable '" + arrayVarName + "' doit être un tableau.");
                continue;
            }

            for (int i = 0; i < rows.size(); i++) {
                Object rowObj = rows.get(i);
                if (!(rowObj instanceof Map<?, ?> row)) {
                    errors.add("Ligne " + (i + 1) + " de '" + arrayVarName + "' doit être un objet.");
                    continue;
                }
                for (String col : expectedColumns) {
                    if (!row.containsKey(col) || row.get(col) == null) {
                        errors.add("Ligne " + (i + 1) + " de '" + arrayVarName + "' : colonne '" + col + "' manquante.");
                    }
                }
            }
        }
        return errors;
    }

    private boolean isTypeValid(String type, Object value) {
        return switch (type) {
            case "STRING", "DATE" -> value instanceof String;
            case "FLOAT" -> isNumeric(value);
            case "BOOLEAN" -> value instanceof Boolean;
            case "ARRAY" -> value instanceof List;
            default -> true;
        };
    }

    private boolean isNumeric(Object value) {
        if (value instanceof Number) return true;
        if (value instanceof String s) {
            try { Double.parseDouble(s); return true; } catch (NumberFormatException e) { return false; }
        }
        return false;
    }
}