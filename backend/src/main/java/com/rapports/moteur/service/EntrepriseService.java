package com.rapports.moteur.service;

import com.rapports.moteur.entity.Role;
import com.rapports.moteur.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EntrepriseService {

    private final HttpServletRequest request;

    public String getCurrentCodeEntreprise() {
        // 1. Vérifier si un utilisateur est authentifié dans le contexte de sécurité
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal) {
            // SUPER_ADMIN peut surcharger le code via le header pour administrer une entreprise spécifique
            if (principal.getRole() == Role.SUPER_ADMIN) {
                String headerCode = request.getHeader("X-Entreprise-Code");
                if (headerCode != null && !headerCode.isBlank()) {
                    return headerCode.trim();
                }
            }
            // Pour tous les utilisateurs rattachés à une entreprise : retour garanti par le token JWT
            if (principal.getEntrepriseCode() != null && !principal.getEntrepriseCode().isBlank()) {
                return principal.getEntrepriseCode();
            }
        }

        // 2. Repli gracieux pour tests unitaires, Swagger ou requêtes de compatibilité
        try {
            String code = request.getHeader("X-Entreprise-Code");
            if (code == null || code.isBlank()) {
                return null;
            }
            return code.trim();
        } catch (Exception e) {
            return null;
        }
    }
}