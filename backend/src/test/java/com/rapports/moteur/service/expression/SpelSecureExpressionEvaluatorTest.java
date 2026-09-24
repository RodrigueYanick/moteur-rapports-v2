package com.rapports.moteur.service.expression;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpelSecureExpressionEvaluatorTest {

    private SpelSecureExpressionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new SpelSecureExpressionEvaluator();
    }

    @Test
    @DisplayName("Évaluation d'arithmétique simple et priorités de calcul")
    void testArithmeticExpressions() {
        Map<String, Object> context = Map.of(
                "qte", 5,
                "prix", 12.5,
                "remise", 10
        );

        Object total = evaluator.evaluate("qte * prix", context);
        assertNotNull(total);
        assertEquals(62.5, ((Number) total).doubleValue(), 0.001);

        Object totalNet = evaluator.evaluate("(qte * prix) * (1 - remise / 100.0)", context);
        assertNotNull(totalNet);
        assertEquals(56.25, ((Number) totalNet).doubleValue(), 0.001);
    }

    @Test
    @DisplayName("Accès sécurisé aux propriétés imbriquées d'un objet / Map")
    void testNestedPropertyAccess() {
        Map<String, Object> context = Map.of(
                "client", Map.of(
                        "nom", "Acme Corp",
                        "adresse", Map.of("ville", "Paris", "codePostal", "75001")
                )
        );

        assertEquals("Acme Corp", evaluator.evaluate("client.nom", context));
        assertEquals("Paris", evaluator.evaluate("client.adresse.ville", context));
        assertEquals("75001", evaluator.evaluate("client.adresse.codePostal", context));
    }

    @Test
    @DisplayName("Fonctions d'aide formatCurrency, formatDate et formatNumber")
    void testHelperFunctions() {
        Map<String, Object> context = Map.of(
                "total", 1250.50,
                "taux", 0.196,
                "dateDoc", "2026-09-15"
        );

        // Avec #
        Object currencyWithHash = evaluator.evaluate("#formatCurrency(total, 'EUR', 'fr-FR')", context);
        assertNotNull(currencyWithHash);
        assertTrue(currencyWithHash.toString().contains("1 250,50") || currencyWithHash.toString().contains("1250,50"));
        assertTrue(currencyWithHash.toString().contains("€"));

        // Sans # (normalisation automatique pour non-techniciens)
        Object currencyWithoutHash = evaluator.evaluate("formatCurrency(total, 'EUR', 'fr-FR')", context);
        assertNotNull(currencyWithoutHash);
        assertEquals(currencyWithHash, currencyWithoutHash);

        // formatDate
        Object formattedDate = evaluator.evaluate("formatDate(dateDoc, 'dd/MM/yyyy')", context);
        assertEquals("15/09/2026", formattedDate);

        // formatNumber
        Object formattedNumber = evaluator.evaluate("formatNumber(taux, 2, 'fr-FR')", context);
        assertTrue(formattedNumber.toString().contains("0,20") || formattedNumber.toString().contains("0.20"));
    }

    @Test
    @DisplayName("Interpolation dans les chaînes evaluateString")
    void testStringInterpolation() {
        Map<String, Object> context = Map.of(
                "nom", "Dupont",
                "qte", 3,
                "prix", 15.0
        );

        String template = "Bonjour {{ nom }}, total: {{ qte * prix }} €.";
        String result = evaluator.evaluateString(template, context);

        assertEquals("Bonjour Dupont, total: 45.0 €.", result);
    }

    @Test
    @DisplayName("Évaluation de conditions booléennes pour le masquage de blocs")
    void testEvaluateCondition() {
        Map<String, Object> context = Map.of(
                "montant", 1500,
                "remise", 0,
                "pays", "FR"
        );

        assertTrue(evaluator.evaluateCondition("montant > 1000", context));
        assertFalse(evaluator.evaluateCondition("remise > 0", context));
        assertTrue(evaluator.evaluateCondition("pays == 'FR' && montant >= 1500", context));
        assertFalse(evaluator.evaluateCondition("pays != 'FR'", context));
        // Condition vide = toujours affiché
        assertTrue(evaluator.evaluateCondition(null, context));
        assertTrue(evaluator.evaluateCondition("", context));
    }

    @Test
    @DisplayName("🛡️ Sécurité Anti-RCE : rejet impératif des types de classe T(...) et constructeurs")
    void testAntiRceProtection() {
        Map<String, Object> context = Map.of("val", 42);

        // Tentative d'accès réflexif via T(...)
        Object resultT = evaluator.evaluate("T(java.lang.Runtime).getRuntime().exec('calc')", context);
        assertNull(resultT, "L'accès réflexif T(...) doit être totalement neutralisé");

        // Tentative d'instanciation de classe via new
        Object resultNew = evaluator.evaluate("new java.io.File('/etc/passwd')", context);
        assertNull(resultNew, "L'instanciation de classes doit être impossible");
    }
}

