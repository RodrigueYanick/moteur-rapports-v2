package com.rapports.moteur.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.schedule.ScheduledJobExecutionResponse;
import com.rapports.moteur.dto.schedule.ScheduledJobRequest;
import com.rapports.moteur.dto.schedule.ScheduledJobResponse;
import com.rapports.moteur.entity.*;
import com.rapports.moteur.exceptions.TemplateNotFoundException;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.mapper.ScheduledJobMapper;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.repository.ScheduledJobExecutionRepository;
import com.rapports.moteur.repository.ScheduledReportJobRepository;
import com.rapports.moteur.service.rendering.ImageExportResult;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class ScheduledJobService {

    private final ScheduledReportJobRepository jobRepository;
    private final ScheduledJobExecutionRepository executionRepository;
    private final ReportTemplateRepository templateRepository;
    private final ReportGenerationService generationService;
    private final EmailNotificationService emailNotificationService;
    private final WebhookDeliveryService webhookDeliveryService;
    private final EntrepriseService entrepriseService;
    private final ScheduledJobMapper jobMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ScheduledJobService(ScheduledReportJobRepository jobRepository,
                               ScheduledJobExecutionRepository executionRepository,
                               ReportTemplateRepository templateRepository,
                               ReportGenerationService generationService,
                               EmailNotificationService emailNotificationService,
                               WebhookDeliveryService webhookDeliveryService,
                               EntrepriseService entrepriseService,
                               ScheduledJobMapper jobMapper) {
        this.jobRepository = jobRepository;
        this.executionRepository = executionRepository;
        this.templateRepository = templateRepository;
        this.generationService = generationService;
        this.emailNotificationService = emailNotificationService;
        this.webhookDeliveryService = webhookDeliveryService;
        this.entrepriseService = entrepriseService;
        this.jobMapper = jobMapper;
    }

    // ============================================================
    // CRUD Multi-Tenant
    // ============================================================

    @Transactional
    public ScheduledJobResponse createJob(ScheduledJobRequest request) {
        String codeEntreprise = resolveCurrentCodeEntreprise();
        ReportTemplate template = findTemplateForCurrentEntreprise(request.getTemplateId(), codeEntreprise);

        validateCron(request.getCronExpression());
        ZoneId zoneId = resolveZoneId(request.getFuseauHoraire());

        LocalDateTime now = LocalDateTime.now(zoneId);
        LocalDateTime nextExec = calculateNextExecution(request.getCronExpression(), now, zoneId);

        String paramsJson = serializeParams(request.getParametres());
        String emailsStr = joinEmails(request.getDestinatairesEmails());

        ScheduledReportJob job = ScheduledReportJob.builder()
                .template(template)
                .codeEntreprise(codeEntreprise)
                .nom(request.getNom())
                .cronExpression(request.getCronExpression().trim())
                .fuseauHoraire(zoneId.getId())
                .formatExport(request.getFormatExport() != null ? request.getFormatExport() : ReportExportFormat.PDF)
                .parametres(paramsJson)
                .destinatairesEmails(emailsStr)
                .webhookUrl(request.getWebhookUrl())
                .actif(request.getActif() != null ? request.getActif() : true)
                .prochaineExecution(nextExec)
                .dernierStatut("CREE")
                .build();

        ScheduledReportJob saved = jobRepository.save(job);
        log.info("Création de la tâche planifiée '{}' ({}) pour l'entreprise {}", saved.getNom(), saved.getId(), codeEntreprise);
        return jobMapper.toResponse(saved);
    }

    @Transactional
    public ScheduledJobResponse updateJob(UUID id, ScheduledJobRequest request) {
        String codeEntreprise = resolveCurrentCodeEntreprise();
        ScheduledReportJob job = jobRepository.findByIdAndCodeEntreprise(id, codeEntreprise)
                .orElseThrow(() -> new ValidationException("Tâche planifiée introuvable : " + id));

        if (!job.getTemplate().getId().equals(request.getTemplateId())) {
            ReportTemplate newTemplate = findTemplateForCurrentEntreprise(request.getTemplateId(), codeEntreprise);
            job.setTemplate(newTemplate);
        }

        validateCron(request.getCronExpression());
        ZoneId zoneId = resolveZoneId(request.getFuseauHoraire());

        LocalDateTime now = LocalDateTime.now(zoneId);
        LocalDateTime nextExec = calculateNextExecution(request.getCronExpression(), now, zoneId);

        job.setNom(request.getNom());
        job.setCronExpression(request.getCronExpression().trim());
        job.setFuseauHoraire(zoneId.getId());
        job.setFormatExport(request.getFormatExport() != null ? request.getFormatExport() : ReportExportFormat.PDF);
        job.setParametres(serializeParams(request.getParametres()));
        job.setDestinatairesEmails(joinEmails(request.getDestinatairesEmails()));
        job.setWebhookUrl(request.getWebhookUrl());
        job.setActif(request.getActif() != null ? request.getActif() : job.getActif());
        job.setProchaineExecution(nextExec);

        ScheduledReportJob updated = jobRepository.save(job);
        return jobMapper.toResponse(updated);
    }

    @Transactional
    public void deleteJob(UUID id) {
        String codeEntreprise = resolveCurrentCodeEntreprise();
        ScheduledReportJob job = jobRepository.findByIdAndCodeEntreprise(id, codeEntreprise)
                .orElseThrow(() -> new ValidationException("Tâche planifiée introuvable : " + id));
        executionRepository.deleteByJob_Id(job.getId());
        jobRepository.delete(job);
        log.info("Suppression de la tâche planifiée {} ({})", job.getNom(), id);
    }

    @Transactional(readOnly = true)
    public ScheduledJobResponse getJob(UUID id) {
        String codeEntreprise = resolveCurrentCodeEntreprise();
        ScheduledReportJob job = jobRepository.findByIdAndCodeEntreprise(id, codeEntreprise)
                .orElseThrow(() -> new ValidationException("Tâche planifiée introuvable : " + id));
        return jobMapper.toResponse(job);
    }

    @Transactional(readOnly = true)
    public List<ScheduledJobResponse> listJobs() {
        String codeEntreprise = resolveCurrentCodeEntreprise();
        return jobRepository.findByCodeEntrepriseOrderByDateCreationDesc(codeEntreprise).stream()
                .map(jobMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduledJobResponse> listJobsByTemplate(UUID templateId) {
        String codeEntreprise = resolveCurrentCodeEntreprise();
        return jobRepository.findByTemplate_IdAndCodeEntrepriseOrderByDateCreationDesc(templateId, codeEntreprise).stream()
                .map(jobMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduledJobExecutionResponse> getJobExecutions(UUID jobId) {
        String codeEntreprise = resolveCurrentCodeEntreprise();
        jobRepository.findByIdAndCodeEntreprise(jobId, codeEntreprise)
                .orElseThrow(() -> new ValidationException("Tâche planifiée introuvable : " + jobId));

        return executionRepository.findByJob_IdOrderByDateDebutDesc(jobId).stream()
                .map(jobMapper::toExecutionResponse)
                .toList();
    }

    // ============================================================
    // Exécution manuelle & automatique
    // ============================================================

    @Transactional
    public ScheduledJobExecutionResponse runNow(UUID id) {
        String codeEntreprise = resolveCurrentCodeEntreprise();
        ScheduledReportJob job = jobRepository.findByIdAndCodeEntreprise(id, codeEntreprise)
                .orElseThrow(() -> new ValidationException("Tâche planifiée introuvable : " + id));

        return jobMapper.toExecutionResponse(executeJobInternal(job));
    }

    /**
     * Moteur de planification récurrente (ShedLock multi-instances).
     * S'exécute chaque minute pour inspecter les tâches planifiées arrivées à échéance.
     */
    @Scheduled(cron = "0 * * * * *")
    @SchedulerLock(name = "ScheduledReportJob_triggerDueJobs", lockAtLeastFor = "15s", lockAtMostFor = "2m")
    public void triggerDueJobs() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledReportJob> dueJobs = jobRepository.findByActifTrueAndProchaineExecutionBefore(now);
        if (dueJobs.isEmpty()) {
            return;
        }

        log.info("Traitement de {} tâche(s) planifiée(s) arrivée(s) à échéance", dueJobs.size());
        for (ScheduledReportJob job : dueJobs) {
            try {
                executeJobInternal(job);
            } catch (Exception e) {
                log.error("Erreur imprévue lors de l'exécution planifiée du job {}", job.getId(), e);
            }
        }
    }

    @Transactional
    public ScheduledJobExecution executeJobInternal(ScheduledReportJob job) {
        long startMs = System.currentTimeMillis();
        LocalDateTime startTime = LocalDateTime.now();

        ScheduledJobExecution execution = ScheduledJobExecution.builder()
                .job(job)
                .dateDebut(startTime)
                .statut(JobExecutionStatus.EN_COURS)
                .destinatairesNotifies(0)
                .build();
        execution = executionRepository.save(execution);

        try {
            Map<String, Object> data = deserializeParams(job.getParametres());
            byte[] fileBytes;
            String filename;
            String mimeType;

            ReportExportFormat format = job.getFormatExport() != null ? job.getFormatExport() : ReportExportFormat.PDF;
            String cleanNom = job.getNom().replaceAll("[^a-zA-Z0-9._-]", "_");

            switch (format) {
                case EXCEL -> {
                    fileBytes = generationService.generateExcel(job.getTemplate().getId(), data);
                    filename = cleanNom + ".xlsx";
                    mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                }
                case PNG -> {
                    ImageExportResult img = generationService.generateImage(job.getTemplate().getId(), data, "PNG", null, 300, null);
                    fileBytes = img.getData();
                    filename = cleanNom + (img.isZip() ? ".zip" : ".png");
                    mimeType = img.getContentType();
                }
                case JPEG -> {
                    ImageExportResult img = generationService.generateImage(job.getTemplate().getId(), data, "JPEG", null, 150, 0.9f);
                    fileBytes = img.getData();
                    filename = cleanNom + (img.isZip() ? ".zip" : ".jpg");
                    mimeType = img.getContentType();
                }
                case CSV -> {
                    fileBytes = generationService.generateCsv(job.getTemplate().getId(), data, ';');
                    filename = cleanNom + ".csv";
                    mimeType = "text/csv; charset=UTF-8";
                }
                case JSON -> {
                    fileBytes = generationService.generateJson(job.getTemplate().getId(), data);
                    filename = cleanNom + "_data.json";
                    mimeType = "application/json";
                }
                default -> { // PDF
                    fileBytes = generationService.generateSync(job.getTemplate().getId(), data);
                    filename = cleanNom + ".pdf";
                    mimeType = "application/pdf";
                }
            }

            // Envoi par email aux destinataires
            List<String> emails = splitEmails(job.getDestinatairesEmails());
            int sentCount = 0;
            if (!emails.isEmpty()) {
                String dateStr = startTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                String subject = String.format("[Rapport Automatisé] %s", job.getNom());
                String bodyHtml = emailNotificationService.buildReportEmailBody(
                        job.getNom(), job.getCronExpression(), job.getCodeEntreprise(), dateStr);
                sentCount = emailNotificationService.sendReportEmail(
                        emails, subject, bodyHtml, filename, mimeType, fileBytes);
            }

            // Déclenchement optionnel du Webhook
            if (job.getWebhookUrl() != null && !job.getWebhookUrl().isBlank()) {
                Map<String, Object> webhookPayload = Map.of(
                        "jobId", job.getId().toString(),
                        "jobNom", job.getNom(),
                        "templateId", job.getTemplate().getId().toString(),
                        "codeEntreprise", job.getCodeEntreprise(),
                        "format", format.name(),
                        "dateExecution", startTime.toString(),
                        "tailleOctets", fileBytes.length
                );
                webhookDeliveryService.sendWebhook(job.getWebhookUrl(), null, "report.scheduled.completed", webhookPayload);
            }

            // Clôture avec succès
            long duration = System.currentTimeMillis() - startMs;
            execution.setDateFin(LocalDateTime.now());
            execution.setStatut(JobExecutionStatus.SUCCES);
            execution.setDestinatairesNotifies(sentCount);
            execution.setDureeMs(duration);

            job.setDerniereExecution(startTime);
            job.setDernierStatut("SUCCES");

            // Recalcul de la prochaine échéance
            ZoneId zoneId = resolveZoneId(job.getFuseauHoraire());
            job.setProchaineExecution(calculateNextExecution(job.getCronExpression(), LocalDateTime.now(zoneId), zoneId));
            jobRepository.save(job);

            return executionRepository.save(execution);

        } catch (Exception e) {
            log.error("Échec lors de l'exécution de la tâche planifiée {}", job.getId(), e);
            long duration = System.currentTimeMillis() - startMs;
            execution.setDateFin(LocalDateTime.now());
            execution.setStatut(JobExecutionStatus.ECHEC);
            execution.setMessageErreur(e.getMessage());
            execution.setDureeMs(duration);

            job.setDerniereExecution(startTime);
            job.setDernierStatut("ECHEC");

            // Recalcul de la prochaine échéance quand même
            ZoneId zoneId = resolveZoneId(job.getFuseauHoraire());
            job.setProchaineExecution(calculateNextExecution(job.getCronExpression(), LocalDateTime.now(zoneId), zoneId));
            jobRepository.save(job);

            return executionRepository.save(execution);
        }
    }

    // ============================================================
    // Helpers & Validation
    // ============================================================

    private void validateCron(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            throw new ValidationException("L'expression CRON ne peut pas être vide");
        }
        if (!CronExpression.isValidExpression(cronExpression.trim())) {
            throw new ValidationException(String.format("L'expression CRON '%s' est invalide", cronExpression));
        }
    }

    private LocalDateTime calculateNextExecution(String cron, LocalDateTime from, ZoneId zoneId) {
        CronExpression parsed = CronExpression.parse(cron.trim());
        ZonedDateTime zdt = from.atZone(zoneId);
        ZonedDateTime next = parsed.next(zdt);
        return next != null ? next.toLocalDateTime() : null;
    }

    private ZoneId resolveZoneId(String fuseau) {
        if (fuseau == null || fuseau.isBlank()) {
            return ZoneId.of("UTC");
        }
        try {
            return ZoneId.of(fuseau.trim());
        } catch (Exception e) {
            return ZoneId.of("UTC");
        }
    }

    private String resolveCurrentCodeEntreprise() {
        String code = entrepriseService.getCurrentCodeEntreprise();
        if (code == null || code.isBlank()) {
            return "ENT-001";
        }
        return code;
    }

    private ReportTemplate findTemplateForCurrentEntreprise(UUID templateId, String codeEntreprise) {
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new TemplateNotFoundException("Template introuvable : " + templateId));

        if (template.getCodeEntreprise() != null && !template.getCodeEntreprise().isBlank()
                && !template.getCodeEntreprise().equals(codeEntreprise)) {
            throw new TemplateNotFoundException("Template introuvable : " + templateId);
        }
        return template;
    }

    private String serializeParams(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, Object> deserializeParams(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private String joinEmails(List<String> emails) {
        if (emails == null || emails.isEmpty()) return null;
        return String.join(",", emails);
    }

    private List<String> splitEmails(String emailsStr) {
        if (emailsStr == null || emailsStr.isBlank()) return Collections.emptyList();
        return Arrays.stream(emailsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
