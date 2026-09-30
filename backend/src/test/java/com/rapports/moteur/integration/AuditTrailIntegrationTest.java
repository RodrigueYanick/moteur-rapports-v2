package com.rapports.moteur.integration;

import com.rapports.moteur.dto.dtoAudit.AuditLogResponse;
import com.rapports.moteur.dto.dtoAudit.AuditStatsResponse;
import com.rapports.moteur.entity.AuditLog;
import com.rapports.moteur.entity.Role;
import com.rapports.moteur.entity.User;
import com.rapports.moteur.repository.AuditLogRepository;
import com.rapports.moteur.security.UserPrincipal;
import com.rapports.moteur.service.EntrepriseService;
import com.rapports.moteur.service.audit.AuditTrailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditTrailIntegrationTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private EntrepriseService entrepriseService;

    @InjectMocks
    private AuditTrailService auditTrailService;

    private UserPrincipal adminPrincipal;
    private final String CODE_ENTREPRISE = "ENT-001";

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("auditor@test.com")
                .nom("Auditor")
                .role(Role.ADMIN_ENTREPRISE)
                .actif(true)
                .build();
        adminPrincipal = new UserPrincipal(user);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities())
        );
    }

    @Test
    @DisplayName("Enregistrement de piste d'audit avec contexte utilisateur complet")
    void shouldLogActionWithFullUserContext() {
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn(CODE_ENTREPRISE);
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> {
            AuditLog a = i.getArgument(0);
            a.setId(UUID.randomUUID());
            a.setDateCreation(LocalDateTime.now());
            return a;
        });

        AuditLog log = auditTrailService.log("DOCUMENT_DOWNLOAD", "DOCUMENT", "DOC-42", "{\"format\":\"PDF\"}", "SUCCES");

        assertThat(log).isNotNull();
        assertThat(log.getAction()).isEqualTo("DOCUMENT_DOWNLOAD");
        assertThat(log.getRessourceType()).isEqualTo("DOCUMENT");
        assertThat(log.getRessourceId()).isEqualTo("DOC-42");
        assertThat(log.getUserEmail()).isEqualTo("auditor@test.com");
        assertThat(log.getUserRole()).isEqualTo("ADMIN_ENTREPRISE");
        assertThat(log.getCodeEntreprise()).isEqualTo(CODE_ENTREPRISE);

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Recherche multi-critères paginée dans l'audit trail")
    void shouldSearchAuditLogsWithFilters() {
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn(CODE_ENTREPRISE);

        AuditLog a1 = AuditLog.builder()
                .id(UUID.randomUUID())
                .codeEntreprise(CODE_ENTREPRISE)
                .userEmail("auditor@test.com")
                .action("TEMPLATE_UPDATE")
                .ressourceType("TEMPLATE")
                .ressourceId("TMPL-1")
                .dateCreation(LocalDateTime.now())
                .statut("SUCCES")
                .build();

        when(auditLogRepository.searchAuditLogs(
                eq(CODE_ENTREPRISE), eq("TEMPLATE_UPDATE"), eq("TEMPLATE"), any(), any(), any(), any()
        )).thenReturn(new PageImpl<>(List.of(a1)));

        Page<AuditLogResponse> result = auditTrailService.searchLogs(
                "TEMPLATE_UPDATE", "TEMPLATE", null, null, null, PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAction()).isEqualTo("TEMPLATE_UPDATE");
        assertThat(result.getContent().get(0).getRessourceId()).isEqualTo("TMPL-1");
    }

    @Test
    @DisplayName("Agrégation des statistiques d'audit pour le tableau de bord")
    void shouldReturnAuditStats() {
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn(CODE_ENTREPRISE);
        when(auditLogRepository.countByCodeEntreprise(CODE_ENTREPRISE)).thenReturn(42L);

        List<Object[]> grouped = List.of(
                new Object[]{"DOCUMENT_GENERATE", 30L},
                new Object[]{"TEMPLATE_UPDATE", 12L}
        );
        when(auditLogRepository.countByActionGrouped(CODE_ENTREPRISE)).thenReturn(grouped);

        AuditStatsResponse stats = auditTrailService.getStats();

        assertThat(stats.getTotalLogs()).isEqualTo(42L);
        assertThat(stats.getCountByAction()).containsEntry("DOCUMENT_GENERATE", 30L);
        assertThat(stats.getCountByAction()).containsEntry("TEMPLATE_UPDATE", 12L);
    }
}