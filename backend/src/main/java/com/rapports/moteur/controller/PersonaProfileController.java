package com.rapports.moteur.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.ApiResponse;
import com.rapports.moteur.dto.dtoProfile.PersonaProfileDto;
import com.rapports.moteur.entity.PersonaProfile;
import com.rapports.moteur.entity.User;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.repository.PersonaProfileRepository;
import com.rapports.moteur.repository.UserRepository;
import com.rapports.moteur.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
@Tag(name = "Profil Universel", description = "Gestion du profil universel de données réutilisables (Mes Données)")
public class PersonaProfileController {

    private final PersonaProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @GetMapping
    @Operation(summary = "Récupérer le profil universel de l'utilisateur connecté",
               description = "Retourne les informations personnelles, expériences, formations et compétences enregistrées pour pré-remplissage automatique des modèles.")
    public ResponseEntity<ApiResponse<PersonaProfileDto>> getCurrentUserProfile() {
        User user = getAuthenticatedUser();

        PersonaProfile profile = profileRepository.findByUser_Id(user.getId())
                .orElseGet(() -> buildDefaultProfile(user));

        PersonaProfileDto dto = toDto(profile);
        return ResponseEntity.ok(ApiResponse.success("Profil universel récupéré", dto));
    }

    @PutMapping
    @org.springframework.transaction.annotation.Transactional
    @Operation(summary = "Mettre à jour le profil universel de l'utilisateur connecté",
               description = "Enregistre ou met à jour les données réutilisables (Identité, Expériences, Formations, Compétences).")
    public ResponseEntity<ApiResponse<PersonaProfileDto>> updateCurrentUserProfile(
            @RequestBody PersonaProfileDto dto) {
        User user = getAuthenticatedUser();

        PersonaProfile profile = profileRepository.findByUser_Id(user.getId())
                .orElseGet(() -> PersonaProfile.builder().user(user).build());

        try {
            if (dto.getPersonalInfo() != null) {
                profile.setPersonalInfo(objectMapper.writeValueAsString(dto.getPersonalInfo()));
            }
            if (dto.getExperiences() != null) {
                profile.setExperiences(objectMapper.writeValueAsString(dto.getExperiences()));
            }
            if (dto.getFormations() != null) {
                profile.setFormations(objectMapper.writeValueAsString(dto.getFormations()));
            }
            if (dto.getCompetences() != null) {
                profile.setCompetences(objectMapper.writeValueAsString(dto.getCompetences()));
            }
            if (dto.getPreferences() != null) {
                profile.setPreferences(objectMapper.writeValueAsString(dto.getPreferences()));
            }
            profile.setDateModification(LocalDateTime.now());

            profile = profileRepository.save(profile);
            return ResponseEntity.ok(ApiResponse.success("Profil universel mis à jour avec succès", toDto(profile)));
        } catch (Exception e) {
            log.error("Erreur sérialisation profil universel : {}", e.getMessage());
            throw new ValidationException(List.of("Erreur de format des données du profil : " + e.getMessage()));
        }
    }

    private User getAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ValidationException(List.of("Utilisateur non authentifié"));
        }
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ValidationException(List.of("Utilisateur introuvable")));
    }

    private PersonaProfile buildDefaultProfile(User user) {
        Map<String, Object> initialInfo = new HashMap<>();
        initialInfo.put("nom", user.getNom());
        initialInfo.put("prenom", user.getPrenom());
        initialInfo.put("email", user.getEmail());
        if (user.getTargetPersona() != null) {
            initialInfo.put("persona", user.getTargetPersona().name());
        }

        try {
            return PersonaProfile.builder()
                    .user(user)
                    .personalInfo(objectMapper.writeValueAsString(initialInfo))
                    .experiences("[]")
                    .formations("[]")
                    .competences("{}")
                    .preferences("{}")
                    .dateModification(LocalDateTime.now())
                    .build();
        } catch (Exception e) {
            return PersonaProfile.builder()
                    .user(user)
                    .dateModification(LocalDateTime.now())
                    .build();
        }
    }

    private PersonaProfileDto toDto(PersonaProfile p) {
        Map<String, Object> personalInfo = deserializeMap(p.getPersonalInfo());
        Object experiences = deserializeGeneric(p.getExperiences(), List.class);
        Object formations = deserializeGeneric(p.getFormations(), List.class);
        Object competences = deserializeGeneric(p.getCompetences(), Map.class);
        Map<String, Object> preferences = deserializeMap(p.getPreferences());

        return PersonaProfileDto.builder()
                .personalInfo(personalInfo)
                .experiences(experiences)
                .formations(formations)
                .competences(competences)
                .preferences(preferences)
                .dateModification(p.getDateModification())
                .build();
    }

    private Map<String, Object> deserializeMap(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(json);
            if (node.isTextual()) {
                node = objectMapper.readTree(node.asText());
            }
            if (node.isObject()) {
                return objectMapper.convertValue(node, Map.class);
            }
            return new HashMap<>();
        } catch (Exception e) {
            log.warn("Erreur désérialisation map: {} | json: {}", e.getMessage(), json);
            return new HashMap<>();
        }
    }

    private Object deserializeGeneric(String json, Class<?> clazz) {
        if (json == null || json.isBlank()) return clazz == List.class ? List.of() : Map.of();
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(json);
            if (node.isTextual()) {
                node = objectMapper.readTree(node.asText());
            }
            return objectMapper.treeToValue(node, clazz);
        } catch (Exception e) {
            log.warn("Erreur désérialisation generic: {} | json: {}", e.getMessage(), json);
            return clazz == List.class ? List.of() : Map.of();
        }
    }
}
