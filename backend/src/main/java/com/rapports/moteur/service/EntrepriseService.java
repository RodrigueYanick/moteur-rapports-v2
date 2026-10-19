package com.rapports.moteur.service;

import com.rapports.moteur.dto.dtoAuth.EnterpriseUpgradeRequest;
import com.rapports.moteur.dto.dtoAuth.LoginResponse;
import com.rapports.moteur.dto.dtoAuth.UserResponse;
import com.rapports.moteur.entity.AccountType;
import com.rapports.moteur.entity.Entreprise;
import com.rapports.moteur.entity.Role;
import com.rapports.moteur.entity.User;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.repository.DocumentRepository;
import com.rapports.moteur.repository.EntrepriseRepository;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.repository.UserRepository;
import com.rapports.moteur.security.JwtTokenProvider;
import com.rapports.moteur.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EntrepriseService {

    private final HttpServletRequest request;
    private final UserRepository userRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final DocumentRepository documentRepository;
    private final ReportTemplateRepository templateRepository;
    private final JwtTokenProvider tokenProvider;

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
            // Pour les utilisateurs personnels sans entreprise : isolation par compte utilisateur
            if (principal.getAccountType() == com.rapports.moteur.entity.AccountType.PERSONAL) {
                return "USER-" + principal.getId();
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

    /**
     * Permet à un utilisateur personnel de transformer son compte en Compte Entreprise,
     * en créant une organisation ou en rejoignant une organisation existante,
     * tout en conservant et migrant l'intégralité de ses documents personnels.
     */
    @Transactional
    public LoginResponse upgradeToEnterprise(UUID userId, EnterpriseUpgradeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException(List.of("Utilisateur introuvable avec l'ID : " + userId)));

        String nom = request.getNomEntreprise().trim();
        String requestedCode = request.getCodeEntreprise() != null && !request.getCodeEntreprise().isBlank()
                ? request.getCodeEntreprise().trim().toUpperCase()
                : null;

        Entreprise entreprise;
        if (requestedCode != null) {
            entreprise = entrepriseRepository.findByCode(requestedCode)
                    .orElseGet(() -> entrepriseRepository.save(
                            Entreprise.builder()
                                    .code(requestedCode)
                                    .nom(nom)
                                    .emailContact(request.getEmailContact() != null ? request.getEmailContact().trim() : null)
                                    .actif(true)
                                    .build()
                    ));
        } else {
            String generatedCode;
            do {
                generatedCode = "ENT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            } while (entrepriseRepository.existsByCode(generatedCode));

            entreprise = entrepriseRepository.save(
                    Entreprise.builder()
                            .code(generatedCode)
                            .nom(nom)
                            .emailContact(request.getEmailContact() != null ? request.getEmailContact().trim() : null)
                            .actif(true)
                            .build()
            );
        }

        // 1. Mettre à jour les droits et l'affectation de l'utilisateur
        user.setAccountType(AccountType.ENTERPRISE);
        user.setRole(Role.ADMIN_ENTREPRISE);
        user.setEntreprise(entreprise);
        userRepository.save(user);

        // 2. Migrer les documents et modèles personnels créés précédemment
        String oldPersonalCode = "USER-" + userId;
        documentRepository.updateCodeEntreprise(oldPersonalCode, entreprise.getCode());
        templateRepository.updateCodeEntreprise(oldPersonalCode, entreprise.getCode());

        // 3. Émettre un nouveau jeton JWT enrichi avec les nouvelles prérogatives
        String newToken = tokenProvider.generateToken(user);

        return LoginResponse.builder()
                .token(newToken)
                .tokenType("Bearer")
                .user(UserResponse.fromEntity(user))
                .build();
    }
}