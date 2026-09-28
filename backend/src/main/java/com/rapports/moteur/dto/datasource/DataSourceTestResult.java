package com.rapports.moteur.dto.datasource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceTestResult {

    private boolean succes;
    private String message;
    private long tempsReponseMs;
    private Object donneesApercu;
}
