package com.rapports.moteur.dto.datasource;

import com.rapports.moteur.entity.DataSourceAuthType;
import com.rapports.moteur.entity.DataSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceRequest {

    @NotBlank(message = "Le nom de la source de données est obligatoire")
    @Size(max = 150, message = "Le nom ne doit pas dépasser 150 caractères")
    private String nom;

    @NotNull(message = "Le type de source de données est obligatoire")
    private DataSourceType type;

    @NotBlank(message = "L'URL ou l'hôte est obligatoire")
    @Size(max = 500, message = "L'URL ou l'hôte ne doit pas dépasser 500 caractères")
    private String urlOuHote;

    private Integer port;

    private String nomBase;

    private String nomUtilisateur;

    // Mot de passe en clair (sera chiffré avant persistance)
    private String motDePasse;

    private String enTetesJson;

    @Builder.Default
    private String methodeHttp = "GET";

    @Builder.Default
    private DataSourceAuthType authType = DataSourceAuthType.NONE;

    private String apiKeyHeader;

    @Builder.Default
    private Integer timeoutSecondes = 10;

    @Builder.Default
    private Boolean actif = true;
}
