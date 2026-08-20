package com.rapports.moteur.service;

import com.rapports.moteur.exceptions.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DataValidatorServiceTest {

    private DataValidatorService validator;

    @BeforeEach
    void setUp() {
        validator = new DataValidatorService(new SchemaExtractorService());
    }

    private static final String SCHEMA_SIMPLE = """
        [
          { "nomVariable": "nom_client", "type": "STRING", "obligatoire": true },
          { "nomVariable": "montant", "type": "FLOAT", "obligatoire": true },
          { "nomVariable": "commentaire", "type": "STRING", "obligatoire": false }
        ]
        """;

    @Test
    void doitPasserAvecDonneesValides() {
        Map<String, Object> data = Map.of("nom_client", "Dupont", "montant", 1500.0);
        assertDoesNotThrow(() -> validator.validate(SCHEMA_SIMPLE, data));
    }

    @Test
    void doitEchouerSiChampObligatoireManquant() {
        Map<String, Object> data = Map.of("montant", 1500.0);
        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(SCHEMA_SIMPLE, data));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("nom_client")));
    }

    @Test
    void doitEchouerSiTypeIncorrect() {
        Map<String, Object> data = Map.of("nom_client", "Dupont", "montant", "pas-un-nombre");
        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(SCHEMA_SIMPLE, data));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("montant")));
    }

    @Test
    void doitAccepterMontantSousFormeDeChaineNumerique() {
        Map<String, Object> data = Map.of("nom_client", "Dupont", "montant", "1500.50");
        assertDoesNotThrow(() -> validator.validate(SCHEMA_SIMPLE, data));
    }

    private static final String DESIGN_TABLEAU_DYNAMIQUE = """
        {
          "pages": [{
            "blocs": [{
              "type": "tableau",
              "source": "lignes_commande",
              "colonnes": [
                { "titre": "Désignation", "variable": "designation" },
                { "titre": "Qte", "variable": "qte" },
                { "titre": "Total", "variable": "total", "formule": "qte * pu" }
              ]
            }]
          }]
        }
        """;

    @Test
    void doitEchouerSiLigneDeTableauIncomplete() {
        // Ligne manquant la colonne non-calculée "qte"
        List<Map<String, Object>> lignes = List.of(
                Map.of("designation", "Produit A")
        );
        Map<String, Object> data = new HashMap<>();
        data.put("lignes_commande", lignes);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(null, data, DESIGN_TABLEAU_DYNAMIQUE));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("lignes_commande")));
    }

    @Test
    void doitIgnorerColonneCalculeeParFormule() {
        // "total" a une formule -> ne doit jamais être exigé dans les données.
        // On fournit une ligne incomplète (manque "qte") pour déclencher une erreur,
        // puis on vérifie que l'erreur ne mentionne pas "total" (colonne calculée ignorée).
        List<Map<String, Object>> lignes = List.of(
                Map.of("designation", "Produit A")
        );
        Map<String, Object> data = new HashMap<>();
        data.put("lignes_commande", lignes);

        ValidationException ex = assertThrows(ValidationException.class,
                () -> validator.validate(null, data, DESIGN_TABLEAU_DYNAMIQUE));
        assertTrue(ex.getErrors().stream().noneMatch(e -> e.contains("'total'")));
    }

    @Test
    void doitPasserAvecTableauDynamiqueComplet() {
        // Toutes les colonnes non-calculées sont présentes ("designation" et "qte"),
        // "total" est ignoré car calculé par formule.
        List<Map<String, Object>> lignes = List.of(
                Map.of("designation", "Produit A", "qte", 2)
        );
        Map<String, Object> data = new HashMap<>();
        data.put("lignes_commande", lignes);

        assertDoesNotThrow(() -> validator.validate(null, data, DESIGN_TABLEAU_DYNAMIQUE));
    }
}