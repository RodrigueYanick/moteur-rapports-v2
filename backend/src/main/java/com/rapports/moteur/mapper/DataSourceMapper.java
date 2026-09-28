package com.rapports.moteur.mapper;

import com.rapports.moteur.dto.datasource.DataSourceRequest;
import com.rapports.moteur.dto.datasource.DataSourceResponse;
import com.rapports.moteur.entity.DataSourceConfig;
import org.springframework.stereotype.Component;

@Component
public class DataSourceMapper {

    public DataSourceResponse toResponse(DataSourceConfig entity) {
        if (entity == null) {
            return null;
        }

        return DataSourceResponse.builder()
                .id(entity.getId())
                .codeEntreprise(entity.getCodeEntreprise())
                .nom(entity.getNom())
                .type(entity.getType())
                .urlOuHote(entity.getUrlOuHote())
                .port(entity.getPort())
                .nomBase(entity.getNomBase())
                .nomUtilisateur(entity.getNomUtilisateur())
                .aMotDePasse(entity.getMotDePasseChiffre() != null && !entity.getMotDePasseChiffre().isBlank())
                .enTetesJson(entity.getEnTetesJson())
                .methodeHttp(entity.getMethodeHttp())
                .authType(entity.getAuthType())
                .apiKeyHeader(entity.getApiKeyHeader())
                .timeoutSecondes(entity.getTimeoutSecondes())
                .actif(entity.getActif())
                .dateCreation(entity.getDateCreation())
                .dateModification(entity.getDateModification())
                .build();
    }

    public DataSourceConfig toEntity(DataSourceRequest request, String codeEntreprise, String motDePasseChiffre) {
        if (request == null) {
            return null;
        }

        return DataSourceConfig.builder()
                .codeEntreprise(codeEntreprise)
                .nom(request.getNom())
                .type(request.getType())
                .urlOuHote(request.getUrlOuHote())
                .port(request.getPort())
                .nomBase(request.getNomBase())
                .nomUtilisateur(request.getNomUtilisateur())
                .motDePasseChiffre(motDePasseChiffre)
                .enTetesJson(request.getEnTetesJson())
                .methodeHttp(request.getMethodeHttp() != null ? request.getMethodeHttp() : "GET")
                .authType(request.getAuthType())
                .apiKeyHeader(request.getApiKeyHeader())
                .timeoutSecondes(request.getTimeoutSecondes() != null ? request.getTimeoutSecondes() : 10)
                .actif(request.getActif() != null ? request.getActif() : true)
                .build();
    }
}
