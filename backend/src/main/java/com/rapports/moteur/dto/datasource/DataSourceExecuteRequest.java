package com.rapports.moteur.dto.datasource;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceExecuteRequest {

    @NotBlank(message = "La requête ou le chemin d'accès est obligatoire")
    private String query;

    private Map<String, Object> parametres;

    @Builder.Default
    private Integer maxRows = 100;
}
