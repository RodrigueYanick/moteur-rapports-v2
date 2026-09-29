package com.rapports.moteur.mapper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.schedule.ScheduledJobExecutionResponse;
import com.rapports.moteur.dto.schedule.ScheduledJobResponse;
import com.rapports.moteur.entity.ScheduledJobExecution;
import com.rapports.moteur.entity.ScheduledReportJob;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ScheduledJobMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ScheduledJobResponse toResponse(ScheduledReportJob job) {
        if (job == null) return null;

        Map<String, Object> params = Collections.emptyMap();
        if (job.getParametres() != null && !job.getParametres().isBlank()) {
            try {
                params = objectMapper.readValue(job.getParametres(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {}
        }

        List<String> emails = Collections.emptyList();
        if (job.getDestinatairesEmails() != null && !job.getDestinatairesEmails().isBlank()) {
            emails = Arrays.stream(job.getDestinatairesEmails().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        return ScheduledJobResponse.builder()
                .id(job.getId())
                .templateId(job.getTemplate() != null ? job.getTemplate().getId() : null)
                .templateNom(job.getTemplate() != null ? job.getTemplate().getNom() : null)
                .codeEntreprise(job.getCodeEntreprise())
                .nom(job.getNom())
                .cronExpression(job.getCronExpression())
                .fuseauHoraire(job.getFuseauHoraire())
                .formatExport(job.getFormatExport())
                .parametres(params)
                .destinatairesEmails(emails)
                .webhookUrl(job.getWebhookUrl())
                .actif(job.getActif())
                .prochaineExecution(job.getProchaineExecution())
                .derniereExecution(job.getDerniereExecution())
                .dernierStatut(job.getDernierStatut())
                .dateCreation(job.getDateCreation())
                .dateModification(job.getDateModification())
                .build();
    }

    public ScheduledJobExecutionResponse toExecutionResponse(ScheduledJobExecution execution) {
        if (execution == null) return null;

        return ScheduledJobExecutionResponse.builder()
                .id(execution.getId())
                .jobId(execution.getJob() != null ? execution.getJob().getId() : null)
                .dateDebut(execution.getDateDebut())
                .dateFin(execution.getDateFin())
                .statut(execution.getStatut())
                .destinatairesNotifies(execution.getDestinatairesNotifies())
                .dureeMs(execution.getDureeMs())
                .messageErreur(execution.getMessageErreur())
                .build();
    }
}
