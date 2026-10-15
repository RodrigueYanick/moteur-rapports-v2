package com.rapports.moteur.service;

import com.rapports.moteur.dto.dtoAuth.LoginRequest;
import com.rapports.moteur.dto.dtoAuth.LoginResponse;
import com.rapports.moteur.dto.dtoAuth.RegisterRequest;
import com.rapports.moteur.dto.dtoAuth.UserResponse;
import com.rapports.moteur.entity.Entreprise;
import com.rapports.moteur.entity.Role;
import com.rapports.moteur.entity.User;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.repository.EntrepriseRepository;
import com.rapports.moteur.repository.UserRepository;
import com.rapports.moteur.security.JwtTokenProvider;
import com.rapports.moteur.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ValidationException(List.of("Identifiants invalides (email ou mot de passe incorrect)")));

        if (!Boolean.TRUE.equals(user.getActif())) {
            throw new ValidationException(List.of("Ce compte utilisateur est désactivé."));
        }

        if (!passwordEncoder.matches(request.getMotDePasse(), user.getMotDePasse())) {
            throw new ValidationException(List.of("Identifiants invalides (email ou mot de passe incorrect)"));
        }

        String token = tokenProvider.generateToken(user);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(UserResponse.fromEntity(user))
                .build();
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ValidationException(List.of("Un utilisateur avec cet email existe déjà : " + email));
        }

        com.rapports.moteur.entity.AccountType accountType = request.getAccountType() != null 
                ? request.getAccountType() 
                : (request.getNomEntreprise() != null && !request.getNomEntreprise().isBlank() ? com.rapports.moteur.entity.AccountType.ENTERPRISE : com.rapports.moteur.entity.AccountType.PERSONAL);
        com.rapports.moteur.entity.TargetPersona targetPersona = request.getTargetPersona();

        if (accountType == com.rapports.moteur.entity.AccountType.ENTERPRISE) {
            boolean hasNom = request.getNomEntreprise() != null && !request.getNomEntreprise().isBlank();
            boolean hasCode = request.getCodeEntreprise() != null && !request.getCodeEntreprise().isBlank();
            if (!hasNom && !hasCode) {
                throw new ValidationException(List.of("Le nom de l'entreprise ou le code organisation est obligatoire pour un compte Entreprise"));
            }
        }

        // Résolution ou création de l'entreprise uniquement si mode ENTREPRISE ou si des infos entreprise sont renseignées
        Entreprise entreprise = null;
        if (accountType == com.rapports.moteur.entity.AccountType.ENTERPRISE || (request.getNomEntreprise() != null && !request.getNomEntreprise().isBlank()) || (request.getCodeEntreprise() != null && !request.getCodeEntreprise().isBlank())) {
            String codeEntreprise = (request.getCodeEntreprise() != null && !request.getCodeEntreprise().isBlank())
                    ? request.getCodeEntreprise().trim().toUpperCase()
                    : null;

            if (codeEntreprise != null) {
                entreprise = entrepriseRepository.findByCode(codeEntreprise)
                        .orElseGet(() -> {
                            String nom = (request.getNomEntreprise() != null && !request.getNomEntreprise().isBlank())
                                    ? request.getNomEntreprise().trim()
                                    : "Entreprise " + codeEntreprise;
                            return entrepriseRepository.save(
                                    Entreprise.builder()
                                            .code(codeEntreprise)
                                            .nom(nom)
                                            .actif(true)
                                            .build()
                            );
                        });
            } else if (request.getNomEntreprise() != null && !request.getNomEntreprise().isBlank()) {
                String nom = request.getNomEntreprise().trim();
                String generatedCode = "ENT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
                entreprise = entrepriseRepository.save(
                        Entreprise.builder()
                                .code(generatedCode)
                                .nom(nom)
                                .actif(true)
                                .build()
                );
            }
        }

        Role role = request.getRole() != null 
                ? request.getRole() 
                : (accountType == com.rapports.moteur.entity.AccountType.ENTERPRISE ? Role.ADMIN_ENTREPRISE : Role.DESIGNER);

        User user = User.builder()
                .email(email)
                .motDePasse(passwordEncoder.encode(request.getMotDePasse()))
                .nom(request.getNom().trim())
                .prenom(request.getPrenom() != null ? request.getPrenom().trim() : null)
                .role(role)
                .accountType(accountType)
                .targetPersona(targetPersona)
                .entreprise(entreprise)
                .actif(true)
                .build();

        user = userRepository.save(user);

        String token = tokenProvider.generateToken(user);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(UserResponse.fromEntity(user))
                .build();
    }

    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ValidationException(List.of("Aucun utilisateur authentifié dans la session"));
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ValidationException(List.of("Utilisateur introuvable")));

        return UserResponse.fromEntity(user);
    }
}

