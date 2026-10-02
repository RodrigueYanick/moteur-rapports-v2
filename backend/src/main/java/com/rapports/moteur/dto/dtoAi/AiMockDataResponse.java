package com.rapports.moteur.dto.dtoAi;

import lombok.*;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMockDataResponse {
    private Map<String, Object> data;
    private boolean fromAi;
    private String provider;
    private String message;
}
