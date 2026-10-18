package com.rapports.moteur.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.dtoProfile.PersonaProfileDto;
import com.rapports.moteur.entity.AccountType;
import com.rapports.moteur.entity.Role;
import com.rapports.moteur.entity.TargetPersona;
import com.rapports.moteur.entity.User;
import com.rapports.moteur.repository.PersonaProfileRepository;
import com.rapports.moteur.repository.UserRepository;
import com.rapports.moteur.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Tests d'intégration MockMvc de PersonaProfileController (Profil Universel / Mes Données)")
class PersonaProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonaProfileRepository profileRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
        testUser = userRepository.save(
                User.builder()
                        .email("jean.etudiant." + UUID.randomUUID().toString().substring(0, 6) + "@test.ca")
                        .motDePasse("Password123!")
                        .nom("Tremblay")
                        .prenom("Jean")
                        .role(Role.DESIGNER)
                        .accountType(AccountType.PERSONAL)
                        .targetPersona(TargetPersona.STUDENT)
                        .actif(true)
                        .build()
        );

        UserPrincipal principal = new UserPrincipal(testUser);
        this.auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/user/profile retourne le profil par défaut initialisé depuis le compte utilisateur")
    void testGetCurrentUserProfile_Default() throws Exception {
        mockMvc.perform(get("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.personalInfo.nom", is("Tremblay")))
                .andExpect(jsonPath("$.data.personalInfo.prenom", is("Jean")))
                .andExpect(jsonPath("$.data.personalInfo.email", is(testUser.getEmail())))
                .andExpect(jsonPath("$.data.personalInfo.persona", is("STUDENT")));
    }

    @Test
    @DisplayName("PUT /api/user/profile met à jour les expériences, formations et compétences réutilisables")
    void testUpdateCurrentUserProfile() throws Exception {
        PersonaProfileDto updateDto = PersonaProfileDto.builder()
                .personalInfo(Map.of(
                        "nom", "Tremblay",
                        "prenom", "Jean",
                        "telephone", "+1 514 555 0199",
                        "ville", "Montréal",
                        "pays", "Canada"
                ))
                .experiences(List.of(
                        Map.of("titre", "Stagiaire Développeur", "entreprise", "Desjardins", "annee", "2024")
                ))
                .formations(List.of(
                        Map.of("diplome", "Baccalauréat Informatique", "etablissement", "Polytechnique Montréal", "annee", "2025")
                ))
                .competences(Map.of(
                        "techniques", List.of("Java", "Spring Boot", "Angular"),
                        "langues", List.of("Français", "Anglais")
                ))
                .preferences(Map.of("theme", "modern_blue"))
                .build();

        mockMvc.perform(put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.personalInfo.ville", is("Montréal")))
                .andExpect(jsonPath("$.data.personalInfo.telephone", is("+1 514 555 0199")))
                .andExpect(jsonPath("$.data.experiences[0].titre", is("Stagiaire Développeur")))
                .andExpect(jsonPath("$.data.formations[0].etablissement", is("Polytechnique Montréal")));

        // Ré-assurer le contexte de sécurité pour le mockMvc subséquent
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Vérification avec GET subséquent
        mockMvc.perform(get("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.personalInfo.ville", is("Montréal")))
                .andExpect(jsonPath("$.data.experiences[0].entreprise", is("Desjardins")));
    }
}
