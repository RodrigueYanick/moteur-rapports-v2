package com.rapports.moteur.dto.schedule;

import com.rapports.moteur.entity.ReportExportFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledJobResponse {
    private UUID id;
    private UUID templateId;
    private String templateNom;
    private String codeEntreprise;
    private String nom;
    private String cronExpression;
    private String fuseauHoraire;
    private ReportExportFormat formatExport;
    private Map<String, Object> parametres;
    private List<String> destinatairesEmails;
    private String webhookUrl;
    private Boolean actif;
    private LocalDateTime prochaineExecution;
    private LocalDateTime derniereExecution;
    private String dernierStatut;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}
