package com.rapports.moteur.service;

import com.rapports.moteur.dto.dtoTemplate.TemplateResponse;
import com.rapports.moteur.dto.dtoWorkflow.WorkflowActionRequest;
import com.rapports.moteur.dto.dtoWorkflow.WorkflowHistoryResponse;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.entity.Role;
import com.rapports.moteur.entity.TemplateStatus;
import com.rapports.moteur.entity.TemplateWorkflowHistory;
import com.rapports.moteur.entity.User;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.mapper.TemplateMapper;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.repository.TemplateWorkflowHistoryRepository;
import com.rapports.moteur.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TemplateWorkflowServiceTest {

    @Mock
    private ReportTemplateRepository templateRepository;

    @Mock
    private TemplateWorkflowHistoryRepository historyRepository;

    @Mock
    private EntrepriseService entrepriseService;

    @Mock
    private TemplateMapper templateMapper;

    @Mock
    private EmailNotificationService emailNotificationService;

    @InjectMocks
    private TemplateWorkflowService workflowService;

    private ReportTemplate template;
    private UserPrincipal adminPrincipal;
    private UserPrincipal userPrincipal;
    private final String CODE_ENTREPRISE = "ENT-001";
    private final UUID TEMPLATE_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        template = ReportTemplate.builder()
                .id(TEMPLATE_ID)
                .nom("Facture Client")
                .codeEntreprise(CODE_ENTREPRISE)
                .statut(TemplateStatus.BROUILLON)
                .build();

        User adminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@test.com")
                .nom("Admin")
                .role(Role.ADMIN_ENTREPRISE)
                .actif(true)
                .build();
        adminPrincipal = new UserPrincipal(adminUser);

        User normalUser = User.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .nom("User")
                .role(Role.DESIGNER)
                .actif(true)
                .build();
        userPrincipal = new UserPrincipal(normalUser);
    }

    private void authenticate(UserPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @Test
    @DisplayName("Soumission pour revue réussie d'un modèle BROUILLON")
    void shouldSubmitForReviewSuccessfully() {
        authenticate(userPrincipal);
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn(CODE_ENTREPRISE);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(ReportTemplate.class))).thenAnswer(i -> i.getArgument(0));
        when(templateMapper.toDto(any(ReportTemplate.class))).thenAnswer(i -> {
            ReportTemplate t = i.getArgument(0);
            return TemplateResponse.builder().id(t.getId()).statut(t.getStatut()).build();
        });

        WorkflowActionRequest request = WorkflowActionRequest.builder().commentaire("Prêt pour vérification").build();
        TemplateResponse response = workflowService.submitForReview(TEMPLATE_ID, request);

        assertThat(response.getStatut()).isEqualTo(TemplateStatus.EN_REVUE);
        assertThat(template.getStatut()).isEqualTo(TemplateStatus.EN_REVUE);
        verify(historyRepository).save(any(TemplateWorkflowHistory.class));
    }

    @Test
    @DisplayName("Échec de la soumission si le modèle n'est pas BROUILLON")
    void shouldFailSubmitWhenNotDraft() {
        authenticate(userPrincipal);
        template.setStatut(TemplateStatus.PUBLIE);
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn(CODE_ENTREPRISE);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> workflowService.submitForReview(TEMPLATE_ID, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Seul un modèle au statut BROUILLON");
    }

    @Test
    @DisplayName("Approbation réussie par un administrateur")
    void shouldApproveSuccessfullyByAdmin() {
        authenticate(adminPrincipal);
        template.setStatut(TemplateStatus.EN_REVUE);
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn(CODE_ENTREPRISE);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(ReportTemplate.class))).thenAnswer(i -> i.getArgument(0));
        when(templateMapper.toDto(any(ReportTemplate.class))).thenAnswer(i -> {
            ReportTemplate t = i.getArgument(0);
            return TemplateResponse.builder().id(t.getId()).statut(t.getStatut()).build();
        });

        TemplateResponse response = workflowService.approveTemplate(TEMPLATE_ID, null);

        assertThat(response.getStatut()).isEqualTo(TemplateStatus.APPROUVE);
        assertThat(template.getStatut()).isEqualTo(TemplateStatus.APPROUVE);
        verify(historyRepository).save(any(TemplateWorkflowHistory.class));
    }

    @Test
    @DisplayName("Rejet de l'approbation si l'utilisateur n'est pas administrateur")
    void shouldRejectApproveByNonAdmin() {
        authenticate(userPrincipal);

        assertThatThrownBy(() -> workflowService.approveTemplate(TEMPLATE_ID, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("réservée aux administrateurs");

        verify(templateRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rejet réussi avec motif par un administrateur")
    void shouldRejectTemplateWithReason() {
        authenticate(adminPrincipal);
        template.setStatut(TemplateStatus.EN_REVUE);
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn(CODE_ENTREPRISE);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(ReportTemplate.class))).thenAnswer(i -> i.getArgument(0));
        when(templateMapper.toDto(any(ReportTemplate.class))).thenAnswer(i -> {
            ReportTemplate t = i.getArgument(0);
            return TemplateResponse.builder().id(t.getId()).statut(t.getStatut()).build();
        });

        WorkflowActionRequest request = WorkflowActionRequest.builder().commentaire("Logo non conforme à la charte").build();
        TemplateResponse response = workflowService.rejectTemplate(TEMPLATE_ID, request);

        assertThat(response.getStatut()).isEqualTo(TemplateStatus.BROUILLON);
        assertThat(template.getStatut()).isEqualTo(TemplateStatus.BROUILLON);
        verify(historyRepository).save(any(TemplateWorkflowHistory.class));
    }

    @Test
    @DisplayName("Rejet échoue si aucun motif n'est fourni")
    void shouldFailRejectWithoutComment() {
        authenticate(adminPrincipal);
        template.setStatut(TemplateStatus.EN_REVUE);
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn(CODE_ENTREPRISE);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> workflowService.rejectTemplate(TEMPLATE_ID, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("motif de rejet est obligatoire");
    }

    @Test
    @DisplayName("Publication officielle en production par un administrateur")
    void shouldPublishSuccessfully() {
        authenticate(adminPrincipal);
        template.setStatut(TemplateStatus.APPROUVE);
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn(CODE_ENTREPRISE);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));
        when(templateRepository.save(any(ReportTemplate.class))).thenAnswer(i -> i.getArgument(0));
        when(templateMapper.toDto(any(ReportTemplate.class))).thenAnswer(i -> {
            ReportTemplate t = i.getArgument(0);
            return TemplateResponse.builder().id(t.getId()).statut(t.getStatut()).build();
        });

        TemplateResponse response = workflowService.publishTemplate(TEMPLATE_ID, null);

        assertThat(response.getStatut()).isEqualTo(TemplateStatus.PUBLIE);
        assertThat(template.getStatut()).isEqualTo(TemplateStatus.PUBLIE);
        verify(historyRepository).save(any(TemplateWorkflowHistory.class));
    }

    @Test
    @DisplayName("Récupération de l'historique du workflow")
    void shouldReturnWorkflowHistory() {
        when(entrepriseService.getCurrentCodeEntreprise()).thenReturn(CODE_ENTREPRISE);
        when(templateRepository.findById(TEMPLATE_ID)).thenReturn(Optional.of(template));

        TemplateWorkflowHistory h1 = TemplateWorkflowHistory.builder()
                .id(UUID.randomUUID())
                .templateId(TEMPLATE_ID)
                .codeEntreprise(CODE_ENTREPRISE)
                .userEmail("admin@test.com")
                .ancienStatut(TemplateStatus.EN_REVUE)
                .nouveauStatut(TemplateStatus.APPROUVE)
                .dateAction(LocalDateTime.now())
                .build();

        when(historyRepository.findByTemplateIdOrderByDateActionDesc(TEMPLATE_ID)).thenReturn(List.of(h1));

        List<WorkflowHistoryResponse> history = workflowService.getWorkflowHistory(TEMPLATE_ID);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).getNouveauStatut()).isEqualTo(TemplateStatus.APPROUVE);
    }
}