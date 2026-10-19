package com.rapports.moteur.controller;

import com.rapports.moteur.dto.ApiResponse;
import com.rapports.moteur.dto.dtoAuth.EnterpriseUpgradeRequest;
import com.rapports.moteur.dto.dtoAuth.LoginResponse;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.security.UserPrincipal;
import com.rapports.moteur.service.EntrepriseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "Gestion Compte Utilisateur", description = "Endpoints de gestion du compte utilisateur et passerelle d'upgrade vers Entreprise")
public class UserAccountController {

    private final EntrepriseService entrepriseService;

    @PostMapping("/upgrade-to-enterprise")
    @Operation(summary = "Passerelle d'upgrade vers Compte Entreprise",
               description = "Permet à un utilisateur personnel de créer son organisation entreprise et de migrer tous ses documents existants sans rupture.")
    public ResponseEntity<LoginResponse> upgradeToEnterprise(
            @Valid @RequestBody EnterpriseUpgradeRequest request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ValidationException(List.of("Authentification requise pour effectuer une mise à niveau vers Entreprise"));
        }

        LoginResponse loginResponse = entrepriseService.upgradeToEnterprise(principal.getId(), request);
        return ResponseEntity.ok(loginResponse);
    }
}
