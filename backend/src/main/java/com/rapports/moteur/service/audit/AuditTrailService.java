package com.rapports.moteur.service.audit;

import com.rapports.moteur.dto.dtoAudit.AuditLogResponse;
import com.rapports.moteur.dto.dtoAudit.AuditStatsResponse;
import com.rapports.moteur.entity.AuditLog;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.repository.AuditLogRepository;
import com.rapports.moteur.security.UserPrincipal;
import com.rapports.moteur.service.EntrepriseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditTrailService {

    private final AuditLogRepository auditLogRepository;
    private final EntrepriseService entrepriseService;

    @Transactional
    public AuditLog log(String action, String ressourceType, String ressourceId, String detailsJson, String statut) {
        String codeEntreprise = null;
        UUID userId = null;
        String userEmail = null;
        String userRole = null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            userId = principal.getId();
            userEmail = principal.getEmail();
            userRole = principal.getRole() != null ? principal.getRole().name() : null;
            codeEntreprise = principal.getEntrepriseCode();
        }

        if (codeEntreprise == null || codeEntreprise.isBlank()) {
            codeEntreprise = entrepriseService.getCurrentCodeEntreprise();
        }

        String clientIp = null;
        String userAgent = null;

        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                clientIp = extractClientIp(request);
                userAgent = request.getHeader("User-Agent");
                if (userAgent != null && userAgent.length() > 250) {
                    userAgent = userAgent.substring(0, 250);
                }
            }
        } catch (Exception e) {
            log.trace("Impossible d'extraire la requête HTTP courante pour l'audit", e);
        }

        AuditLog auditLog = AuditLog.builder()
                .codeEntreprise(codeEntreprise)
                .userId(userId)
                .userEmail(userEmail)
                .userRole(userRole)
                .action(action)
                .ressourceType(ressourceType)
                .ressourceId(ressourceId)
                .adresseIp(clientIp)
                .userAgent(userAgent)
                .detailsJson(detailsJson)
                .statut(statut != null ? statut : "SUCCES")
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.debug("Piste d'audit enregistrée : [action: {}, ressource: {}/{}, user: {}, ip: {}]",
                action, ressourceType, ressourceId, userEmail, clientIp);
        return saved;
    }

    public AuditLog log(String action, String ressourceType, String ressourceId, String detailsJson) {
        return log(action, ressourceType, ressourceId, detailsJson, "SUCCES");
    }

    public AuditLog logAction(String action, String ressourceType, String ressourceId, Map<String, Object> detailsMap) {
        String json = null;
        if (detailsMap != null && !detailsMap.isEmpty()) {
            try {
                json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(detailsMap);
            } catch (Exception e) {
                json = detailsMap.toString();
            }
        }
        try {
            return log(action, ressourceType, ressourceId, json, "SUCCES");
        } catch (Exception e) {
            log.error("Erreur lors de l'enregistrement de l'audit log", e);
            return null;
        }
    }

    public Page<AuditLogResponse> searchLogs(String action, String ressourceType, String userEmail,
                                            LocalDateTime dateDebut, LocalDateTime dateFin, Pageable pageable) {
        String codeEntreprise = entrepriseService.getCurrentCodeEntreprise();
        if (codeEntreprise == null || codeEntreprise.isBlank()) {
            throw new ValidationException(List.of("Code entreprise manquant dans la session"));
        }

        return auditLogRepository.searchAuditLogs(
                codeEntreprise,
                (action != null && !action.isBlank()) ? action.trim() : null,
                (ressourceType != null && !ressourceType.isBlank()) ? ressourceType.trim() : null,
                (userEmail != null && !userEmail.isBlank()) ? userEmail.trim() : null,
                dateDebut,
                dateFin,
                pageable
        ).map(AuditLogResponse::fromEntity);
    }

    public AuditStatsResponse getStats() {
        String codeEntreprise = entrepriseService.getCurrentCodeEntreprise();
        if (codeEntreprise == null || codeEntreprise.isBlank()) {
            return AuditStatsResponse.builder().totalLogs(0).countByAction(Collections.emptyMap()).build();
        }

        long total = auditLogRepository.countByCodeEntreprise(codeEntreprise);
        List<Object[]> rows = auditLogRepository.countByActionGrouped(codeEntreprise);

        Map<String, Long> countMap = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String act = (String) row[0];
            Long count = (Long) row[1];
            countMap.put(act, count);
        }

        return AuditStatsResponse.builder()
                .totalLogs(total)
                .countByAction(countMap)
                .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        String xr = request.getHeader("X-Real-IP");
        if (xr != null && !xr.isBlank()) {
            return xr.trim();
        }
        return request.getRemoteAddr();
    }
}