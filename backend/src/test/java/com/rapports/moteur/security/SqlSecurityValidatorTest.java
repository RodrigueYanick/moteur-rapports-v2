package com.rapports.moteur.security;

import com.rapports.moteur.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SqlSecurityValidatorTest {

    private SqlSecurityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SqlSecurityValidator();
    }

    @Test
    @DisplayName("Requête SELECT simple valide")
    void testValidSelect() {
        assertDoesNotThrow(() -> validator.validateReadOnlyQuery("SELECT id, nom, montant FROM facture WHERE paye = true"));
    }

    @Test
    @DisplayName("Requête SELECT avec jointures et agrégations valide")
    void testValidComplexSelect() {
        String sql = """
            SELECT c.nom, count(f.id) as nb_factures, sum(f.montant) as total
            FROM client c
            INNER JOIN facture f ON f.client_id = c.id
            WHERE f.date >= '2026-01-01'
            GROUP BY c.nom
            HAVING sum(f.montant) > 1000
            ORDER BY total DESC
        """;
        assertDoesNotThrow(() -> validator.validateReadOnlyQuery(sql));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "INSERT INTO client (nom) VALUES ('Hacker')",
        "UPDATE facture SET montant = 0",
        "DELETE FROM document WHERE id = 1",
        "DROP TABLE report_template",
        "ALTER TABLE user ADD COLUMN admin BOOLEAN",
        "TRUNCATE TABLE batch_generation_item",
        "CREATE TABLE test (id INT)"
    })
    @DisplayName("Rejet des instructions SQL d'écriture ou DDL")
    void testRejectNonSelectQueries(String harmfulSql) {
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validateReadOnlyQuery(harmfulSql));
        assertTrue(ex.getMessage().contains("Seules les requêtes SELECT") || ex.getMessage().contains("Requête SQL invalide"));
    }

    @Test
    @DisplayName("Rejet de multi-requêtes (query stacking avec point-virgule)")
    void testRejectStackedQueries() {
        String sql = "SELECT * FROM client; DROP TABLE document;";
        ValidationException ex = assertThrows(ValidationException.class, () -> validator.validateReadOnlyQuery(sql));
        assertTrue(ex.getMessage().contains("Seule une instruction SQL unique"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "SELECT * FROM client WHERE id = 1 AND pg_sleep(5)",
        "SELECT nom FROM client UNION SELECT sleep(5)",
        "SELECT * FROM client INTO OUTFILE '/tmp/dump.txt'",
        "SELECT load_file('/etc/passwd')"
    })
    @DisplayName("Rejet des fonctions malveillantes ou injections temporelles")
    void testRejectDangerousKeywords(String dangerousSql) {
        assertThrows(ValidationException.class, () -> validator.validateReadOnlyQuery(dangerousSql));
    }

    @Test
    @DisplayName("Injection automatique de la clause LIMIT si absente")
    void testEnforceLimitWhenMissing() {
        String sql = "SELECT id, nom FROM produit ORDER BY nom ASC";
        String sanitized = validator.sanitizeAndEnforceLimit(sql, 500);

        assertTrue(sanitized.toUpperCase().contains("LIMIT 500"));
    }

    @Test
    @DisplayName("Conservation de la clause LIMIT existante si elle est inférieure")
    void testKeepExistingLimit() {
        String sql = "SELECT id, nom FROM produit LIMIT 50";
        String sanitized = validator.sanitizeAndEnforceLimit(sql, 500);

        assertTrue(sanitized.toUpperCase().contains("LIMIT 50"));
    }
}
