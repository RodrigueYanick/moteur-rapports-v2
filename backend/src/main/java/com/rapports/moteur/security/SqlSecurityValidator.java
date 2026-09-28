package com.rapports.moteur.security;

import com.rapports.moteur.exceptions.ValidationException;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Validateur de sécurité SQL strict utilisant l'AST de JSqlParser.
 * Garantit que les requêtes exécutées sur les bases distantes sont en lecture seule (SELECT).
 */
@Component
@Slf4j
public class SqlSecurityValidator {

    private static final int DEFAULT_MAX_ROWS = 1000;

    private static final List<String> FORBIDDEN_KEYWORDS = List.of(
            "pg_sleep",
            "sleep(",
            "into outfile",
            "into dumpfile",
            "load_file",
            "information_schema",
            "pg_catalog"
    );

    /**
     * Valide qu'une requête SQL est une instruction SELECT unique, sécurisée et en lecture seule.
     *
     * @param sql Requête SQL fournie par l'utilisateur
     * @throws ValidationException si la requête enfreint les règles de sécurité
     */
    public void validateReadOnlyQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new ValidationException("La requête SQL ne peut pas être vide");
        }

        String trimmed = sql.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);

        // 1. Détection de mots-clés dangereux pré-AST
        for (String forbidden : FORBIDDEN_KEYWORDS) {
            if (lower.contains(forbidden)) {
                log.warn("Rejet de requête SQL contenant le mot-clé interdit '{}' : {}", forbidden, sql);
                throw new ValidationException("La requête contient des fonctions ou tables système non autorisées");
            }
        }

        // 2. Détection de multi-requêtes / query stacking (point-virgule)
        try {
            Statements statements = CCJSqlParserUtil.parseStatements(trimmed);
            if (statements.getStatements().size() != 1) {
                log.warn("Rejet de requête SQL avec instructions multiples ({})", statements.getStatements().size());
                throw new ValidationException("Seule une instruction SQL unique est autorisée par requête");
            }

            Statement stmt = statements.getStatements().get(0);

            // 3. Vérification que l'instruction est strictement un SELECT
            if (!(stmt instanceof Select)) {
                log.warn("Rejet de requête non-SELECT (type: {}) : {}", stmt.getClass().getSimpleName(), sql);
                throw new ValidationException("Seules les requêtes SELECT en lecture seule sont permises");
            }

        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            log.warn("Erreur de parsing SQL : {}", e.getMessage());
            throw new ValidationException("Requête SQL invalide ou non conforme : " + e.getMessage());
        }
    }

    /**
     * Garantit qu'une clause LIMIT est appliquée sur la requête SELECT.
     * Si aucune limite n'est présente, injecte automatiquement 'LIMIT maxRows'.
     *
     * @param sql Requête SQL valide
     * @param maxRows Limite maximale de lignes
     * @return Requête SQL avec LIMIT
     */
    public String sanitizeAndEnforceLimit(String sql, int maxRows) {
        validateReadOnlyQuery(sql);

        int effectiveLimit = (maxRows > 0 && maxRows <= DEFAULT_MAX_ROWS) ? maxRows : DEFAULT_MAX_ROWS;

        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (stmt instanceof Select select) {
                if (select.getSelectBody() instanceof PlainSelect plainSelect) {
                    if (plainSelect.getLimit() == null) {
                        plainSelect.setLimit(new net.sf.jsqlparser.statement.select.Limit().withRowCount(
                                new net.sf.jsqlparser.expression.LongValue(effectiveLimit)
                        ));
                    }
                    return plainSelect.toString();
                }
            }
            return sql;
        } catch (Exception e) {
            log.warn("Impossible d'injecter LIMIT via AST, fallback ajout manuel : {}", e.getMessage());
            if (!sql.toLowerCase(Locale.ROOT).contains("limit")) {
                return sql.trim() + " LIMIT " + effectiveLimit;
            }
            return sql;
        }
    }
}
