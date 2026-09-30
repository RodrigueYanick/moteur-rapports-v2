package com.rapports.moteur.dto.dtoWorkflow;

import com.rapports.moteur.entity.TemplateStatus;
import com.rapports.moteur.entity.TemplateWorkflowHistory;
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
public class WorkflowHistoryResponse {
    private UUID id;
    private UUID templateId;
    private String codeEntreprise;
    private UUID userId;
    private String userEmail;
    private TemplateStatus ancienStatut;
    private TemplateStatus nouveauStatut;
    private String commentaire;
    private LocalDateTime dateAction;

    public static WorkflowHistoryResponse fromEntity(TemplateWorkflowHistory entity) {
        return WorkflowHistoryResponse.builder()
                .id(entity.getId())
                .templateId(entity.getTemplateId())
                .codeEntreprise(entity.getCodeEntreprise())
                .userId(entity.getUserId())
                .userEmail(entity.getUserEmail())
                .ancienStatut(entity.getAncienStatut())
                .nouveauStatut(entity.getNouveauStatut())
                .commentaire(entity.getCommentaire())
                .dateAction(entity.getDateAction())
                .build();
    }
}