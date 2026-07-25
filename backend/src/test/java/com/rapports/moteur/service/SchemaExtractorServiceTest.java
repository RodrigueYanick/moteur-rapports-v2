package com.rapports.moteur.service;

import com.rapports.moteur.dto.dtoVariable.ExtractedVariable;
import com.rapports.moteur.entity.VariableType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaExtractorServiceTest {

    private final SchemaExtractorService service = new SchemaExtractorService();

    @Test
    void extraitLesVariablesDeTitreEtTexteEnStringEtDansLOrdre() {
        String design = """
            { "blocs": [
                { "type": "titre", "contenu": "Rapport {{nom_client}}" },
                { "type": "texte", "contenu": "Le {{date_edition}} pour {{nom_client}}" }
            ]}
            """;

        List<ExtractedVariable> variables = service.extraire(design);

        assertEquals(2, variables.size());
        assertEquals("nom_client", variables.get(0).getNom());
        assertEquals(VariableType.STRING, variables.get(0).getType());
        assertTrue(variables.get(0).getObligatoire());
        assertEquals("date_edition", variables.get(1).getNom());
        assertEquals(VariableType.STRING, variables.get(1).getType());
    }

    @Test
    void extraitLaSourceDUnTableauEnArray() {
        String design = """
            { "blocs": [
                { "type": "tableau", "source": "{{lignes_facture}}",
                  "colonnes": [ { "titre": "Produit", "variable": "produit" } ] }
            ]}
            """;

        List<ExtractedVariable> variables = service.extraire(design);

        assertEquals(1, variables.size());
        assertEquals("lignes_facture", variables.get(0).getNom());
        assertEquals(VariableType.ARRAY, variables.get(0).getType());
    }

    @Test
    void neDupliquePasUneVariableApparaissantPlusieursFois() {
        String design = """
            { "blocs": [
                { "type": "titre", "contenu": "{{nom_client}}" },
                { "type": "texte", "contenu": "Cher {{nom_client}}," }
            ]}
            """;

        List<ExtractedVariable> variables = service.extraire(design);

        assertEquals(1, variables.size());
    }

    @Test
    void ignoreLesColonnesDeTableauQuiNeSontPasEntreAccolades() {
        String design = """
            { "blocs": [
                { "type": "tableau", "source": "{{lignes}}",
                  "colonnes": [ { "titre": "Prix", "variable": "prix_unitaire" } ] }
            ]}
            """;

        List<ExtractedVariable> variables = service.extraire(design);

        assertEquals(1, variables.size());
        assertEquals("lignes", variables.get(0).getNom());
    }

    @Test
    void retourneListeVideSiDesignVideOuNull() {
        assertTrue(service.extraire(null).isEmpty());
        assertTrue(service.extraire("").isEmpty());
    }
}
