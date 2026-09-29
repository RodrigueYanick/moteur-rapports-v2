package com.rapports.moteur.service;

import com.rapports.moteur.dto.schedule.ScheduledJobRequest;
import com.rapports.moteur.dto.schedule.ScheduledJobResponse;
import com.rapports.moteur.entity.*;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.mapper.ScheduledJobMapper;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.repository.ScheduledJobExecutionRepository;
import com.rapports.moteur.repository.ScheduledReportJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduledJobServiceTest {

    @Mock
    private ScheduledReportJobRepository jobRepository;

    @Mock
    private ScheduledJobExecutionRepository executionRepository;

    @Mock
    private ReportTemplateRepository templateRepository;

    @Mock
    private ReportGenerationService generationService;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private WebhookDeliveryService webhookDeliveryService;

    @Mock
    private EntrepriseService entrepriseService;

    @Spy
    private ScheduledJobMapper jobMapper = new ScheduledJobMapper();

    @InjectMocks
    private ScheduledJobService scheduledJobService;

    private ReportTemplate template;
    private UUID templateId;

    @BeforeEach
    void setUp() {
        templateId = UUID.randomUUID();
        template = ReportTemplate.builder()
                .id(templateId)
                .nom("Template Ventes")
                .codeEntreprise("ENT-001")
                .statut(TemplateStatus.PUBLIE)
                .build();
    }

    @Test
    @DisplayName("Devrait créer une tâche planifiée avec calcul de la prochaine exécution")
    void testCreateJobSuccess() {
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn("ENT-001");
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        ScheduledJobRequest request = ScheduledJobRequest.builder()
                .templateId(templateId)
                .nom("Rapport Hebdo Ventes")
                .cronExpression("0 0 8 * * MON")
                .fuseauHoraire("Europe/Paris")
                .formatExport(ReportExportFormat.PDF)
                .destinatairesEmails(List.of("direction@alpha.com"))
                .parametres(Map.of("annee", 2026))
                .build();

        when(jobRepository.save(any(ScheduledReportJob.class))).thenAnswer(invocation -> {
            ScheduledReportJob job = invocation.getArgument(0);
            job.setId(UUID.randomUUID());
            return job;
        });

        ScheduledJobResponse response = scheduledJobService.createJob(request);

        assertThat(response).isNotNull();
        assertThat(response.getNom()).isEqualTo("Rapport Hebdo Ventes");
        assertThat(response.getCronExpression()).isEqualTo("0 0 8 * * MON");
        assertThat(response.getProchaineExecution()).isNotNull();
        assertThat(response.getDestinatairesEmails()).contains("direction@alpha.com");
    }

    @Test
    @DisplayName("Devrait rejeter une expression CRON invalide")
    void testCreateJobInvalidCron() {
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn("ENT-001");
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        ScheduledJobRequest request = ScheduledJobRequest.builder()
                .templateId(templateId)
                .nom("Job Erroné")
                .cronExpression("expression-cron-invalide")
                .build();

        assertThatThrownBy(() -> scheduledJobService.createJob(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("invalide");
    }

    @Test
    @DisplayName("Devrait exécuter une tâche planifiée et enregistrer l'exécution")
    void testExecuteJobInternalSuccess() {
        ScheduledReportJob job = ScheduledReportJob.builder()
                .id(UUID.randomUUID())
                .template(template)
                .codeEntreprise("ENT-001")
                .nom("Rapport Auto")
                .cronExpression("0 0 12 * * *")
                .fuseauHoraire("UTC")
                .formatExport(ReportExportFormat.PDF)
                .destinatairesEmails("boss@alpha.com")
                .build();

        when(executionRepository.save(any(ScheduledJobExecution.class))).thenAnswer(inv -> {
            ScheduledJobExecution ex = inv.getArgument(0);
            ex.setId(UUID.randomUUID());
            return ex;
        });

        when(generationService.generateSync(eq(templateId), any())).thenReturn("%PDF-1.4 test".getBytes());
        lenient().when(emailNotificationService.sendReportEmail(any(), any(), any(), any(), any(), any()))
                .thenReturn(1);

        ScheduledJobExecution execution = scheduledJobService.executeJobInternal(job);

        assertThat(execution.getStatut()).isEqualTo(JobExecutionStatus.SUCCES);
        assertThat(execution.getDestinatairesNotifies()).isEqualTo(1);
        assertThat(job.getDernierStatut()).isEqualTo("SUCCES");
        assertThat(job.getDerniereExecution()).isNotNull();
        assertThat(job.getProchaineExecution()).isNotNull();

        verify(jobRepository).save(job);
    }

    @Test
    @DisplayName("Devrait consigner l'échec en cas d'erreur de génération de document")
    void testExecuteJobInternalFailure() {
        ScheduledReportJob job = ScheduledReportJob.builder()
                .id(UUID.randomUUID())
                .template(template)
                .codeEntreprise("ENT-001")
                .nom("Rapport Défaillant")
                .cronExpression("0 0 12 * * *")
                .fuseauHoraire("UTC")
                .formatExport(ReportExportFormat.PDF)
                .build();

        when(executionRepository.save(any(ScheduledJobExecution.class))).thenAnswer(inv -> {
            ScheduledJobExecution ex = inv.getArgument(0);
            ex.setId(UUID.randomUUID());
            return ex;
        });

        when(generationService.generateSync(eq(templateId), any()))
                .thenThrow(new IllegalStateException("Erreur interne moteur de rendu"));

        ScheduledJobExecution execution = scheduledJobService.executeJobInternal(job);

        assertThat(execution.getStatut()).isEqualTo(JobExecutionStatus.ECHEC);
        assertThat(execution.getMessageErreur()).contains("Erreur interne moteur de rendu");
        assertThat(job.getDernierStatut()).isEqualTo("ECHEC");
        verify(jobRepository).save(job);
    }
}
