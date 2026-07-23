package com.rapports.moteur.service;

import com.rapports.moteur.entity.ReportVariable;
import com.rapports.moteur.entity.VariableType;
import com.rapports.moteur.exceptions.ValidationException;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DataValidatorService {

    public void validate(List<ReportVariable> variables, Map<String, Object> data) {
        List<String> errors = new ArrayList<>();

        for (ReportVariable variable : variables) {
            String nom = variable.getNomVariable();
            Object value = data.get(nom);

            // 1. Champ obligatoire manquant ou explicitement null
            if (Boolean.TRUE.equals(variable.getObligatoire()) && (value == null || data.get(nom) == null)) {
                errors.add("Champ obligatoire manquant : " + nom);
                continue; // pas la peine de vérifier le type si absent
            }

            // 2. Champ optionnel absent → on ignore
            if (value == null) {
                continue;
            }

            // 3. Vérification du type
            if (!isTypeValid(variable.getType(), value)) {
                errors.add("Type invalide pour '" + nom + "' : attendu " + variable.getType());
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private boolean isTypeValid(VariableType type, Object value) {
        return switch (type) {
            case STRING, DATE -> value instanceof String;
            case FLOAT -> value instanceof Number;
            case BOOLEAN -> value instanceof Boolean;
            case ARRAY -> value instanceof List;
        };
    }
}