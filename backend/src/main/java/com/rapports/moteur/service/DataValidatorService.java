package com.rapports.moteur.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.entity.VariableType;
import com.rapports.moteur.exceptions.ValidationException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DataValidatorService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Valide les données par rapport au schéma JSON stocké dans le template.
     * @param schemaJson le contenu de la colonne "schema" (tableau JSON d'ExtractedVariable)
     * @param data       les données soumises pour la génération
     * @throws ValidationException si des erreurs sont détectées
     */
    public void validate(String schemaJson, Map<String, Object> data) {
        if (schemaJson == null || schemaJson.isBlank()) {
            return; // pas de schéma → pas de validation
        }

        List<String> errors = new ArrayList<>();
        try {
            JsonNode array = objectMapper.readTree(schemaJson);
            if (!array.isArray()) return;

            for (JsonNode node : array) {
                String nom = node.path("nomVariable").asText();
                boolean obligatoire = node.path("obligatoire").asBoolean();
                String typeStr = node.path("type").asText();

                Object value = data.get(nom);

                // 1. Champ obligatoire manquant ou explicitement null
                if (obligatoire && (value == null || data.get(nom) == null)) {
                    errors.add("Champ obligatoire manquant : " + nom);
                    continue; // pas la peine de vérifier le type
                }

                // 2. Champ optionnel absent → on ignore
                if (value == null) {
                    continue;
                }

                // 3. Vérification du type
                if (!isTypeValid(typeStr, value)) {
                    errors.add("Type invalide pour '" + nom + "' : attendu " + typeStr);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Erreur lecture schéma JSON", e);
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private boolean isTypeValid(String type, Object value) {
        return switch (type) {
            case "STRING", "DATE" -> value instanceof String;
            case "FLOAT" -> value instanceof Number;
            case "BOOLEAN" -> value instanceof Boolean;
            case "ARRAY" -> value instanceof List;
            default -> true; // type inconnu, on accepte
        };
    }

    private boolean isTypeValid(VariableType type, Object value) {
        return switch (type) {
            case STRING, DATE -> value instanceof String;
            case FLOAT -> isNumeric(value);
            case BOOLEAN -> value instanceof Boolean;
            case ARRAY -> value instanceof List;
        };
    }

    private boolean isNumeric(Object value) {
        if (value instanceof Number) return true;
        if (value instanceof String s) {
            try {
                Double.parseDouble(s);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}