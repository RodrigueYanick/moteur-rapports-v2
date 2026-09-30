package com.rapports.moteur.dto.dtoAudit;

import com.rapports.moteur.entity.AuditLog;
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
public class AuditLogResponse {
    private UUID id;
    private LocalDateTime dateCreation;
    private String codeEntreprise;
    private UUID userId;
    private String userEmail;
    private String userRole;
    private String action;
    private String ressourceType;
    private String ressourceId;
    private String adresseIp;
    private String userAgent;
    private String detailsJson;
    private String statut;

    public static AuditLogResponse fromEntity(AuditLog entity) {
        return AuditLogResponse.builder()
                .id(entity.getId())
                .dateCreation(entity.getDateCreation())
                .codeEntreprise(entity.getCodeEntreprise())
                .userId(entity.getUserId())
                .userEmail(entity.getUserEmail())
                .userRole(entity.getUserRole())
                .action(entity.getAction())
                .ressourceType(entity.getRessourceType())
                .ressourceId(entity.getRessourceId())
                .adresseIp(entity.getAdresseIp())
                .userAgent(entity.getUserAgent())
                .detailsJson(entity.getDetailsJson())
                .statut(entity.getStatut())
                .build();
    }
}