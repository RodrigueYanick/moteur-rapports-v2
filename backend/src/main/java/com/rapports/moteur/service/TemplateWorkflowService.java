package com.rapports.moteur.service;

import com.rapports.moteur.dto.dtoTemplate.TemplateResponse;
import com.rapports.moteur.dto.dtoWorkflow.WorkflowActionRequest;
import com.rapports.moteur.dto.dtoWorkflow.WorkflowHistoryResponse;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.entity.Role;
import com.rapports.moteur.entity.TemplateStatus;
import com.rapports.moteur.entity.TemplateWorkflowHistory;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.mapper.TemplateMapper;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.repository.TemplateWorkflowHistoryRepository;
import com.rapports.moteur.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service de gestion du cycle de vie et du workflow de validation des modèles de rapports :
 * BROUILLON -> EN_REVUE -> APPROUVE -> PUBLIE.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateWorkflowService {

    private final ReportTemplateRepository templateRepository;
    private final TemplateWorkflowHistoryRepository historyRepository;
    private final EntrepriseService entrepriseService;
    private final TemplateMapper templateMapper;
    private final EmailNotificationService emailNotificationService;

    @Transactional
    public TemplateResponse submitForReview(UUID templateId, WorkflowActionRequest request) {
        UserPrincipal user = getCurrentUser();
        ReportTemplate template = findAndVerify(templateId);

        if (template.getStatut() != TemplateStatus.BROUILLON) {
            throw new ValidationException(List.of(
                    String.format("Seul un modèle au statut BROUILLON peut être soumis pour revue (statut actuel: %s)", template.getStatut())
            ));
        }

        TemplateStatus ancien = template.getStatut();
        template.setStatut(TemplateStatus.EN_REVUE);
        ReportTemplate saved = templateRepository.save(template);

        String commentaire = (request != null && request.getCommentaire() != null) ? request.getCommentaire().trim() : "Soumission pour revue";
        logHistory(saved, user, ancien, TemplateStatus.EN_REVUE, commentaire);

        log.info("Modèle '{}' ({}) soumis pour revue par {}", saved.getNom(), saved.getId(), user.getEmail());
        return templateMapper.toDto(saved);
    }

    @Transactional
    public TemplateResponse approveTemplate(UUID templateId, WorkflowActionRequest request) {
        UserPrincipal user = getCurrentUser();
        checkAdmin(user);

        ReportTemplate template = findAndVerify(templateId);

        if (template.getStatut() != TemplateStatus.EN_REVUE) {
            throw new ValidationException(List.of(
                    String.format("Seul un modèle au statut EN_REVUE peut être approuvé (statut actuel: %s)", template.getStatut())
            ));
        }

        TemplateStatus ancien = template.getStatut();
        template.setStatut(TemplateStatus.APPROUVE);
        ReportTemplate saved = templateRepository.save(template);

        String commentaire = (request != null && request.getCommentaire() != null) ? request.getCommentaire().trim() : "Approuvé par l'administrateur";
        logHistory(saved, user, ancien, TemplateStatus.APPROUVE, commentaire);

        log.info("Modèle '{}' ({}) approuvé par {}", saved.getNom(), saved.getId(), user.getEmail());
        return templateMapper.toDto(saved);
    }

    @Transactional
    public TemplateResponse rejectTemplate(UUID templateId, WorkflowActionRequest request) {
        UserPrincipal user = getCurrentUser();
        checkAdmin(user);

        ReportTemplate template = findAndVerify(templateId);

        if (template.getStatut() != TemplateStatus.EN_REVUE) {
            throw new ValidationException(List.of(
                    String.format("Seul un modèle au statut EN_REVUE peut être rejeté (statut actuel: %s)", template.getStatut())
            ));
        }

        if (request == null || request.getCommentaire() == null || request.getCommentaire().trim().isBlank()) {
            throw new ValidationException(List.of("Le motif de rejet est obligatoire"));
        }

        TemplateStatus ancien = template.getStatut();
        template.setStatut(TemplateStatus.BROUILLON);
        ReportTemplate saved = templateRepository.save(template);

        String motif = request.getCommentaire().trim();
        logHistory(saved, user, ancien, TemplateStatus.BROUILLON, "Rejeté : " + motif);

        log.info("Modèle '{}' ({}) rejeté par {}. Motif : {}", saved.getNom(), saved.getId(), user.getEmail(), motif);
        return templateMapper.toDto(saved);
    }

    @Transactional
    public TemplateResponse publishTemplate(UUID templateId, WorkflowActionRequest request) {
        UserPrincipal user = getCurrentUser();
        checkAdmin(user);

        ReportTemplate template = findAndVerify(templateId);

        TemplateStatus ancien = template.getStatut();
        template.setStatut(TemplateStatus.PUBLIE);
        ReportTemplate saved = templateRepository.save(template);

        String commentaire = (request != null && request.getCommentaire() != null) ? request.getCommentaire().trim() : "Publication officielle";
        logHistory(saved, user, ancien, TemplateStatus.PUBLIE, commentaire);

        log.info("Modèle '{}' ({}) publié officiellement par {}", saved.getNom(), saved.getId(), user.getEmail());
        return templateMapper.toDto(saved);
    }

    public List<WorkflowHistoryResponse> getWorkflowHistory(UUID templateId) {
        findAndVerify(templateId); // validation appartenance
        return historyRepository.findByTemplateIdOrderByDateActionDesc(templateId).stream()
                .map(WorkflowHistoryResponse::fromEntity)
                .toList();
    }

    private void logHistory(ReportTemplate template, UserPrincipal user, TemplateStatus ancien, TemplateStatus nouveau, String commentaire) {
        TemplateWorkflowHistory history = TemplateWorkflowHistory.builder()
                .templateId(template.getId())
                .codeEntreprise(template.getCodeEntreprise())
                .userId(user.getId())
                .userEmail(user.getEmail())
                .ancienStatut(ancien)
                .nouveauStatut(nouveau)
                .commentaire(commentaire)
                .build();
        historyRepository.save(history);
    }

    private ReportTemplate findAndVerify(UUID templateId) {
        String codeEntreprise = entrepriseService.getCurrentCodeEntreprise();
        ReportTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new ValidationException(List.of("Modèle introuvable : " + templateId)));

        if (codeEntreprise != null && !codeEntreprise.isBlank() && !codeEntreprise.equals(template.getCodeEntreprise())) {
            throw new ValidationException(List.of("Modèle introuvable ou accès non autorisé"));
        }
        return template;
    }

    private UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ValidationException(List.of("Aucun utilisateur authentifié dans la session"));
        }
        return principal;
    }

    private void checkAdmin(UserPrincipal user) {
        if (user.getRole() != Role.ADMIN_ENTREPRISE && user.getRole() != Role.SUPER_ADMIN) {
            throw new ValidationException(List.of("Opération réservée aux administrateurs de l'entreprise"));
        }
    }
}