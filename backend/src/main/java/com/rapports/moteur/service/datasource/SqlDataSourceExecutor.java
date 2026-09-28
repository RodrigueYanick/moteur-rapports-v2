package com.rapports.moteur.service.datasource;

import com.rapports.moteur.entity.DataSourceConfig;
import com.rapports.moteur.entity.DataSourceType;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.security.SqlSecurityValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class SqlDataSourceExecutor {

    private final SqlSecurityValidator sqlSecurityValidator;

    /**
     * Exécute une requête SELECT sécurisée sur une base de données distante PostgreSQL ou MySQL.
     *
     * @param config Configuration de la source
     * @param motDePasseClair Mot de passe déchiffré
     * @param sql Requête SQL
     * @param parametres Paramètres nominatifs ou positionnels (optionnels)
     * @param maxRows Limite de lignes
     * @return Liste de lignes sous forme de Map<colonne, valeur>
     */
    public List<Map<String, Object>> executeSelect(
            DataSourceConfig config,
            String motDePasseClair,
            String sql,
            Map<String, Object> parametres,
            int maxRows
    ) {
        String sanitizedSql = sqlSecurityValidator.sanitizeAndEnforceLimit(sql, maxRows);
        String jdbcUrl = buildJdbcUrl(config);

        int timeout = (config.getTimeoutSecondes() != null && config.getTimeoutSecondes() > 0)
                ? config.getTimeoutSecondes() : 10;

        DriverManager.setLoginTimeout(timeout);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, config.getNomUtilisateur(), motDePasseClair)) {
            conn.setReadOnly(true);
            conn.setAutoCommit(true);

            try (PreparedStatement stmt = conn.prepareStatement(sanitizedSql)) {
                stmt.setQueryTimeout(timeout);
                stmt.setMaxRows(maxRows > 0 ? maxRows : 1000);

                try (ResultSet rs = stmt.executeQuery()) {
                    return mapResultSetToList(rs);
                }
            }
        } catch (SQLException e) {
            log.error("Erreur lors de l'exécution SQL sur la source '{}' ({}) : {}",
                    config.getNom(), config.getType(), e.getMessage());
            throw new ValidationException("Échec de la requête sur la base distante : " + e.getMessage());
        }
    }

    /**
     * Teste la connectivité avec la base SQL (ping / SELECT 1).
     */
    public boolean testConnection(DataSourceConfig config, String motDePasseClair) {
        String jdbcUrl = buildJdbcUrl(config);
        int timeout = (config.getTimeoutSecondes() != null && config.getTimeoutSecondes() > 0)
                ? config.getTimeoutSecondes() : 5;

        DriverManager.setLoginTimeout(timeout);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, config.getNomUtilisateur(), motDePasseClair)) {
            conn.setReadOnly(true);
            return conn.isValid(timeout);
        } catch (Exception e) {
            log.warn("Test de connectivité échoué pour la source '{}' : {}", config.getNom(), e.getMessage());
            throw new ValidationException("Impossible de se connecter à la base : " + e.getMessage());
        }
    }

    private String buildJdbcUrl(DataSourceConfig config) {
        String host = config.getUrlOuHote();
        // Si l'utilisateur a collé une JDBC URL complète
        if (host.startsWith("jdbc:")) {
            return host;
        }

        if (config.getType() == DataSourceType.POSTGRESQL) {
            int port = (config.getPort() != null && config.getPort() > 0) ? config.getPort() : 5432;
            String db = (config.getNomBase() != null && !config.getNomBase().isBlank()) ? config.getNomBase() : "postgres";
            return String.format("jdbc:postgresql://%s:%d/%s?connectTimeout=%d&socketTimeout=%d",
                    host, port, db,
                    config.getTimeoutSecondes() != null ? config.getTimeoutSecondes() : 10,
                    config.getTimeoutSecondes() != null ? config.getTimeoutSecondes() : 10);
        } else if (config.getType() == DataSourceType.MYSQL) {
            int port = (config.getPort() != null && config.getPort() > 0) ? config.getPort() : 3306;
            String db = (config.getNomBase() != null && !config.getNomBase().isBlank()) ? config.getNomBase() : "";
            return String.format("jdbc:mysql://%s:%d/%s?connectTimeout=%d&socketTimeout=%d&useSSL=false&allowPublicKeyRetrieval=true",
                    host, port, db,
                    (config.getTimeoutSecondes() != null ? config.getTimeoutSecondes() : 10) * 1000,
                    (config.getTimeoutSecondes() != null ? config.getTimeoutSecondes() : 10) * 1000);
        }

        throw new IllegalArgumentException("Type de base SQL non supporté : " + config.getType());
    }

    private List<Map<String, Object>> mapResultSetToList(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>(colCount);
            for (int i = 1; i <= colCount; i++) {
                String colName = meta.getColumnLabel(i);
                Object val = rs.getObject(i);
                if (val instanceof java.sql.Date d) {
                    val = d.toLocalDate().toString();
                } else if (val instanceof java.sql.Timestamp ts) {
                    val = ts.toLocalDateTime().toString();
                }
                row.put(colName, val);
            }
            rows.add(row);
        }
        return rows;
    }
}
