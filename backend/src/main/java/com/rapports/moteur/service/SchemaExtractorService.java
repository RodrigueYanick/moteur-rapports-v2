package com.rapports.moteur.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.dtoVariable.ExtractedVariable;
import com.rapports.moteur.entity.VariableType;
import com.rapports.moteur.exceptions.ValidationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extrait automatiquement, à partir du design JSON d'un template (contenuDesign),
 * la liste des variables qu'il référence :
 * - tous les {{nom}} présents dans le "contenu" des blocs "titre" / "texte" (type STRING)
 * - la variable "source" des blocs "tableau", qui désigne une liste de lignes (type ARRAY)
 *
 * Les "variable" de colonnes d'un tableau ne sont PAS extraites ici : ce sont de simples
 * clés de lecture dans chaque ligne de la liste "source", pas des champs {{...}} à
 * compléter directement par l'utilisateur.
 */
@Service
public class SchemaExtractorService {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(.+?)\\}\\}");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ExtractedVariable> extraire(String contenuDesignJson) {
        if (contenuDesignJson == null || contenuDesignJson.isBlank()) {
            return List.of();
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(contenuDesignJson);
        } catch (Exception e) {
            throw new ValidationException(List.of("contenuDesign n'est pas un JSON valide : " + e.getMessage()));
        }

        // LinkedHashMap : déduplique par nom tout en conservant l'ordre de première apparition
        Map<String, VariableType> variablesDetectees = new LinkedHashMap<>();

        for (JsonNode bloc : root.path("blocs")) {
            String type = bloc.path("type").asText("");
            switch (type) {
                case "titre", "texte" ->
                        extraireDuTexte(bloc.path("contenu").asText(""), variablesDetectees);
                case "tableau" -> {
                    String source = extraireNomVariable(bloc.path("source").asText(""));
                    if (!source.isBlank()) {
                        // Toujours ARRAY : une source de tableau désigne une liste de lignes
                        variablesDetectees.put(source, VariableType.ARRAY);
                    }
                }
                default -> {
                    // Type de bloc inconnu : on l'ignore silencieusement
                }
            }
        }

        List<ExtractedVariable> resultat = new ArrayList<>();
        for (Map.Entry<String, VariableType> entry : variablesDetectees.entrySet()) {
            resultat.add(ExtractedVariable.builder()
                    .nom(entry.getKey())
                    .type(entry.getValue())
                    .obligatoire(true) // toute variable référencée dans le design est nécessaire au rendu
                    .build());
        }
        return resultat;
    }

    private void extraireDuTexte(String contenu, Map<String, VariableType> destination) {
        Matcher matcher = VAR_PATTERN.matcher(contenu);
        while (matcher.find()) {
            String nom = matcher.group(1).trim();
            if (nom.isBlank()) continue;
            // On ne rétrograde jamais un ARRAY déjà détecté (cas tableau) vers un STRING
            destination.putIfAbsent(nom, VariableType.STRING);
        }
    }

    private String extraireNomVariable(String source) {
        Matcher matcher = VAR_PATTERN.matcher(source);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return source.replace("{{", "").replace("}}", "").trim();
    }
}
