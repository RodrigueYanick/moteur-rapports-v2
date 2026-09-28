package com.rapports.moteur.dto.datasource;

import com.rapports.moteur.entity.DataSourceAuthType;
import com.rapports.moteur.entity.DataSourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceTestRequest {

    // Option 1 : ID d'une source existante
    private UUID dataSourceId;

    // Option 2 : Données saisies à la volée avant enregistrement
    private DataSourceType type;
    private String urlOuHote;
    private Integer port;
    private String nomBase;
    private String nomUtilisateur;
    private String motDePasse;
    private String enTetesJson;
    private String methodeHttp;
    private DataSourceAuthType authType;
    private String apiKeyHeader;
    private Integer timeoutSecondes;

    // Requête de test optionnelle (ex: 'SELECT 1' ou path REST)
    private String testQuery;
}
