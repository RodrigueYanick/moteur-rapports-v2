package com.rapports.moteur.integration;

import com.rapports.moteur.dto.schedule.ScheduledJobRequest;
import com.rapports.moteur.entity.*;
import com.rapports.moteur.repository.ScheduledJobExecutionRepository;
import com.rapports.moteur.repository.ScheduledReportJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Tests d'Intégration : Planificateur de Rapports CRON & Diffusion")
class ScheduledJobIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ScheduledReportJobRepository jobRepository;

    @Autowired
    private ScheduledJobExecutionRepository executionRepository;

    private Entreprise entreprise1;
    private Entreprise entreprise2;
    private User admin1;
    private User admin2;
    private String token1;
    private String token2;
    private ReportTemplate template1;
    private ReportTemplate template2;

    @BeforeEach
    void setUp() {
        executionRepository.deleteAll();
        jobRepository.deleteAll();

        entreprise1 = createEntreprise("ENT-001", "Entreprise Alpha");
        entreprise2 = createEntreprise("ENT-002", "Entreprise Beta");

        admin1 = createUser("admin1@alpha.com", "Password123!", Role.ADMIN_ENTREPRISE, entreprise1);
        admin2 = createUser("admin2@beta.com", "Password123!", Role.ADMIN_ENTREPRISE, entreprise2);

        token1 = getBearerToken(admin1);
        token2 = getBearerToken(admin2);

        template1 = createTemplate("Rapport Mensuel Alpha", "ENT-001", TemplateStatus.PUBLIE);
        template1.setContenuDesign("{\"pages\":[{\"blocs\":[{\"type\":\"text\",\"contenu\":\"Facture {{client}}\"}]}]}");
        templateRepository.save(template1);

        template2 = createTemplate("Rapport Beta", "ENT-002", TemplateStatus.PUBLIE);
        template2.setContenuDesign("{\"pages\":[{\"blocs\":[{\"type\":\"text\",\"contenu\":\"Rapport {{client}}\"}]}]}");
        templateRepository.save(template2);
    }

    @Test
    @DisplayName("Cycle complet : Création, consultation, exécution immédiate, historique et suppression")
    void testFullScheduledJobLifecycle() throws Exception {
        ScheduledJobRequest request = ScheduledJobRequest.builder()
                .templateId(template1.getId())
                .nom("Diffusion Hebdo Factures")
                .cronExpression("0 0 8 * * MON")
                .fuseauHoraire("Europe/Paris")
                .formatExport(ReportExportFormat.PDF)
                .parametres(Map.of("client", "Acme Corporation"))
                .destinatairesEmails(List.of("directeur@alpha.com", "compta@alpha.com"))
                .build();

        // 1. Création
        String responseJson = mockMvc.perform(post("/api/schedules")
                        .header("Authorization", token1)
                        .header("X-Entreprise-Code", "ENT-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom", is("Diffusion Hebdo Factures")))
                .andExpect(jsonPath("$.formatExport", is("PDF")))
                .andExpect(jsonPath("$.prochaineExecution", notNullValue()))
                .andReturn().getResponse().getContentAsString();

        UUID jobId = UUID.fromString(objectMapper.readTree(responseJson).get("id").asText());

        // 2. Consultation
        mockMvc.perform(get("/api/schedules/" + jobId)
                        .header("Authorization", token1)
                        .header("X-Entreprise-Code", "ENT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(jobId.toString())))
                .andExpect(jsonPath("$.destinatairesEmails", hasSize(2)));

        // 3. Exécution immédiate (Run Now)
        mockMvc.perform(post("/api/schedules/" + jobId + "/run-now")
                        .header("Authorization", token1)
                        .header("X-Entreprise-Code", "ENT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("SUCCES")))
                .andExpect(jsonPath("$.jobId", is(jobId.toString())));

        // 4. Consultation de l'historique des exécutions
        mockMvc.perform(get("/api/schedules/" + jobId + "/executions")
                        .header("Authorization", token1)
                        .header("X-Entreprise-Code", "ENT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].statut", is("SUCCES")));

        // 5. Suppression
        mockMvc.perform(delete("/api/schedules/" + jobId)
                        .header("Authorization", token1)
                        .header("X-Entreprise-Code", "ENT-001"))
                .andExpect(status().isNoContent());

        // 6. Vérifier la disparition
        mockMvc.perform(get("/api/schedules/" + jobId)
                        .header("Authorization", token1)
                        .header("X-Entreprise-Code", "ENT-001"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Isolation Multi-Tenant : Une entreprise ne peut ni voir ni exécuter les jobs d'une autre")
    void testMultiTenancyIsolation() throws Exception {
        ScheduledJobRequest request = ScheduledJobRequest.builder()
                .templateId(template1.getId())
                .nom("Secret Job Alpha")
                .cronExpression("0 0 12 * * *")
                .build();

        String responseJson = mockMvc.perform(post("/api/schedules")
                        .header("Authorization", token1)
                        .header("X-Entreprise-Code", "ENT-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID jobId = UUID.fromString(objectMapper.readTree(responseJson).get("id").asText());

        // Entreprise 2 tente d'accéder au job d'Entreprise 1
        mockMvc.perform(get("/api/schedules/" + jobId)
                        .header("Authorization", token2)
                        .header("X-Entreprise-Code", "ENT-002"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("introuvable")));

        // Entreprise 2 tente d'exécuter le job d'Entreprise 1
        mockMvc.perform(post("/api/schedules/" + jobId + "/run-now")
                        .header("Authorization", token2)
                        .header("X-Entreprise-Code", "ENT-002"))
                .andExpect(status().isBadRequest());

        // La liste d'Entreprise 2 doit être vide
        mockMvc.perform(get("/api/schedules")
                        .header("Authorization", token2)
                        .header("X-Entreprise-Code", "ENT-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Validation : Rejet de création avec expression CRON invalide")
    void testRejectInvalidCron() throws Exception {
        ScheduledJobRequest request = ScheduledJobRequest.builder()
                .templateId(template1.getId())
                .nom("Bad Cron Job")
                .cronExpression("not a valid cron")
                .build();

        mockMvc.perform(post("/api/schedules")
                        .header("Authorization", token1)
                        .header("X-Entreprise-Code", "ENT-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("invalide")));
    }
}
