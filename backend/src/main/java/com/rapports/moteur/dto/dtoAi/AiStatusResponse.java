package com.rapports.moteur.dto.dtoAi;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiStatusResponse {
    private boolean enabled;
    private boolean available;
    private String provider;
    private String model;
}
