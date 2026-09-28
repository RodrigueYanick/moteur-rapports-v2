package com.rapports.moteur.dto.datasource;

import com.rapports.moteur.entity.DataSourceAuthType;
import com.rapports.moteur.entity.DataSourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceResponse {

    private UUID id;
    private String codeEntreprise;
    private String nom;
    private DataSourceType type;
    private String urlOuHote;
    private Integer port;
    private String nomBase;
    private String nomUtilisateur;
    private boolean aMotDePasse;
    private String enTetesJson;
    private String methodeHttp;
    private DataSourceAuthType authType;
    private String apiKeyHeader;
    private Integer timeoutSecondes;
    private Boolean actif;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}
