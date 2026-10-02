package com.rapports.moteur.dto.dtoAi;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiMockDataRequest {
    private UUID templateId;
    private String templateNom;
    private List<VariableItem> variables;
    private List<String> arrayColumns;
    private Integer rowCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariableItem {
        private String nom;
        private String type;
        private String description;
    }
}
