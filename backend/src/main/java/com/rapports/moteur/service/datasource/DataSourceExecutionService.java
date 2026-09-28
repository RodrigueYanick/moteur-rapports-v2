package com.rapports.moteur.service.datasource;

import com.rapports.moteur.entity.DataSourceConfig;
import com.rapports.moteur.entity.DataSourceType;
import com.rapports.moteur.security.crypto.AesCryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSourceExecutionService {

    private final SqlDataSourceExecutor sqlExecutor;
    private final RestApiDataSourceExecutor restExecutor;
    private final AesCryptoService cryptoService;

    /**
     * Exécute une requête ou un appel API sur une source de données configurée.
     */
    public Object execute(
            DataSourceConfig config,
            String queryOrPath,
            Map<String, Object> parametres,
            int maxRows
    ) {
        String secretClair = cryptoService.decrypt(config.getMotDePasseChiffre());

        if (config.getType() == DataSourceType.POSTGRESQL || config.getType() == DataSourceType.MYSQL) {
            return sqlExecutor.executeSelect(config, secretClair, queryOrPath, parametres, maxRows);
        } else if (config.getType() == DataSourceType.REST_API) {
            return restExecutor.executeRest(config, secretClair, queryOrPath, parametres);
        }

        throw new IllegalArgumentException("Type de source de données non pris en charge : " + config.getType());
    }

    /**
     * Teste la connectivité avec la source distante.
     */
    public boolean testConnection(DataSourceConfig config, String testQuery) {
        String secretClair = cryptoService.decrypt(config.getMotDePasseChiffre());

        if (config.getType() == DataSourceType.POSTGRESQL || config.getType() == DataSourceType.MYSQL) {
            return sqlExecutor.testConnection(config, secretClair);
        } else if (config.getType() == DataSourceType.REST_API) {
            return restExecutor.testConnection(config, secretClair, testQuery);
        }

        return false;
    }
}
