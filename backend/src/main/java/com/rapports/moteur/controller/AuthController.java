package com.rapports.moteur.controller;

import com.rapports.moteur.dto.ApiError;
import com.rapports.moteur.dto.dtoAuth.LoginRequest;
import com.rapports.moteur.dto.dtoAuth.LoginResponse;
import com.rapports.moteur.dto.dtoAuth.RegisterRequest;
import com.rapports.moteur.dto.dtoAuth.UserResponse;
import com.rapports.moteur.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Endpoints d'authentification JWT, inscription et gestion de profil utilisateur")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Connexion utilisateur", description = "Authentifie un utilisateur avec son email et mot de passe et renvoie un jeton JWT.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Connexion réussie",
                     content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "400", description = "Identifiants invalides ou compte inactif",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Inscription utilisateur", description = "Crée un nouvel utilisateur et son entreprise (si spécifiée), puis retourne un jeton JWT.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Compte créé avec succès",
                     content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "400", description = "Email déjà utilisé ou données invalides",
                     content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Profil de l'utilisateur connecté", description = "Retourne les informations de l'utilisateur connecté basé sur son jeton Bearer JWT.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profil utilisateur retourné",
                     content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "401", description = "Non authentifié ou jeton expiré")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }
}

