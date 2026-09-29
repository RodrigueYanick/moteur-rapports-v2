package com.rapports.moteur.dto.schedule;

import com.rapports.moteur.entity.ReportExportFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledJobRequest {

    @NotNull(message = "L'identifiant du template est obligatoire")
    private UUID templateId;

    @NotBlank(message = "Le nom de la tâche planifiée est obligatoire")
    private String nom;

    @NotBlank(message = "L'expression CRON est obligatoire")
    @Schema(description = "Expression CRON Spring (ex: '0 0 8 * * MON' pour tous les lundis à 8h)", example = "0 0 8 * * MON")
    private String cronExpression;

    @Builder.Default
    private String fuseauHoraire = "UTC";

    @Builder.Default
    private ReportExportFormat formatExport = ReportExportFormat.PDF;

    private Map<String, Object> parametres;

    private List<String> destinatairesEmails;

    private String webhookUrl;

    @Builder.Default
    private Boolean actif = true;
}
