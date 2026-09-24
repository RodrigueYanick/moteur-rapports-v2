package com.rapports.moteur.integration;

import com.rapports.moteur.dto.dtoAuth.LoginRequest;
import com.rapports.moteur.dto.dtoAuth.RegisterRequest;
import com.rapports.moteur.entity.Entreprise;
import com.rapports.moteur.entity.Role;
import com.rapports.moteur.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Tests d'Intégration : Authentification, Sécurité et Profil Utilisateur")
class AuthSecurityIntegrationTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("1. Cas Nominaux (Happy Path)")
    class HappyPathTests {

        @Test
        @DisplayName("Inscription nominale d'un utilisateur et création de son entreprise")
        void shouldRegisterNewUserSuccessfully() throws Exception {
            RegisterRequest request = RegisterRequest.builder()
                    .email("alice@acme.com")
                    .motDePasse("Password123!")
                    .nom("Smith")
                    .prenom("Alice")
                    .role(Role.DESIGNER)
                    .codeEntreprise("ACME_CORP")
                    .nomEntreprise("Acme Corporation")
                    .build();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").isString())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.user.email").value("alice@acme.com"))
                    .andExpect(jsonPath("$.user.nom").value("Smith"))
                    .andExpect(jsonPath("$.user.prenom").value("Alice"))
                    .andExpect(jsonPath("$.user.role").value("DESIGNER"))
                    .andExpect(jsonPath("$.user.codeEntreprise").value("ACME_CORP"));

            // Vérification de la persistance réelle en base de données
            User persistedUser = userRepository.findByEmail("alice@acme.com").orElse(null);
            assertThat(persistedUser).isNotNull();
            assertThat(persistedUser.getNom()).isEqualTo("Smith");
            assertThat(persistedUser.getActif()).isTrue();
            // Le mot de passe doit être hashé avec BCrypt, jamais en clair
            assertThat(persistedUser.getMotDePasse()).isNotEqualTo("Password123!");
            assertThat(passwordEncoder.matches("Password123!", persistedUser.getMotDePasse())).isTrue();

            Entreprise persistedEntreprise = entrepriseRepository.findByCode("ACME_CORP").orElse(null);
            assertThat(persistedEntreprise).isNotNull();
            assertThat(persistedEntreprise.getNom()).isEqualTo("Acme Corporation");
        }

        @Test
        @DisplayName("Connexion nominale d'un utilisateur existant et génération d'un JWT valide")
        void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
            Entreprise ent = createEntreprise("BETA_CORP", "Beta Corporation");
            createUser("bob@beta.com", "Secret456!", Role.DESIGNER, ent);

            LoginRequest loginRequest = LoginRequest.builder()
                    .email("bob@beta.com")
                    .motDePasse("Secret456!")
                    .build();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.user.email").value("bob@beta.com"))
                    .andExpect(jsonPath("$.user.codeEntreprise").value("BETA_CORP"));
        }

        @Test
        @DisplayName("Récupération du profil courant (/api/auth/me) avec un jeton Bearer valide")
        void shouldReturnCurrentUserProfileWhenAuthenticated() throws Exception {
            Entreprise ent = createEntreprise("GAMMA_CORP", "Gamma Corporation");
            User user = createUser("carol@gamma.com", "Secret789!", Role.ADMIN_ENTREPRISE, ent);
            String token = getBearerToken(user);

            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(user.getId().toString()))
                    .andExpect(jsonPath("$.email").value("carol@gamma.com"))
                    .andExpect(jsonPath("$.role").value("ADMIN_ENTREPRISE"))
                    .andExpect(jsonPath("$.codeEntreprise").value("GAMMA_CORP"));
        }
    }

    @Nested
    @DisplayName("2. Sécurité & Contrôle d'Accès")
    class SecurityAccessTests {

        @Test
        @DisplayName("Rejet 401 Unauthorized pour un utilisateur anonyme sur /api/auth/me")
        void shouldRejectAnonymousAccessToAuthMe() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Rejet 401 Unauthorized pour un jeton JWT altéré / invalide")
        void shouldRejectInvalidJwtToken() throws Exception {
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Rejet 401 Unauthorized sur les endpoints protégés de l'application sans jeton")
        void shouldRejectAnonymousAccessToProtectedEndpoints() throws Exception {
            mockMvc.perform(get("/api/templates"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/api/workspace-config"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/api/batches"))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/api/documents"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Accès public autorisé sans jeton sur /api/health")
        void shouldAllowPublicAccessToHealthCheck() throws Exception {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }
    }

    @Nested
    @DisplayName("3. Cas aux Limites & Gestion des Erreurs (Edge Cases)")
    class EdgeCasesAndErrorsTests {

        @Test
        @DisplayName("Échec d'inscription : email mal formaté ou champs obligatoires manquants (400 Bad Request)")
        void shouldFailRegisterWithInvalidInputs() throws Exception {
            RegisterRequest invalidRequest = RegisterRequest.builder()
                    .email("not-an-email")
                    .motDePasse("")
                    .nom("")
                    .build();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message", containsString("email")));
        }

        @Test
        @DisplayName("Échec d'inscription : conflit d'email déjà utilisé (400 Bad Request)")
        void shouldFailRegisterWhenEmailAlreadyExists() throws Exception {
            Entreprise ent = createEntreprise("EXIST_ENT", "Existing Entreprise");
            createUser("existing@acme.com", "Password123!", Role.DESIGNER, ent);

            RegisterRequest duplicate = RegisterRequest.builder()
                    .email("existing@acme.com")
                    .motDePasse("AnotherPass123!")
                    .nom("Other")
                    .build();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(duplicate)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Un utilisateur avec cet email")));
        }

        @Test
        @DisplayName("Échec de connexion : mauvais mot de passe (400 Bad Request)")
        void shouldFailLoginWithWrongPassword() throws Exception {
            Entreprise ent = createEntreprise("LOGIN_ENT", "Login Entreprise");
            createUser("user@login.com", "GoodPassword123!", Role.DESIGNER, ent);

            LoginRequest wrongPass = LoginRequest.builder()
                    .email("user@login.com")
                    .motDePasse("WrongPassword123!")
                    .build();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(wrongPass)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Identifiants invalides")));
        }

        @Test
        @DisplayName("Échec de connexion : email inexistant (400 Bad Request)")
        void shouldFailLoginWithNonExistentEmail() throws Exception {
            LoginRequest notFound = LoginRequest.builder()
                    .email("ghost@unknown.com")
                    .motDePasse("SomePassword123!")
                    .build();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(notFound)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Identifiants invalides")));
        }

        @Test
        @DisplayName("Échec de connexion : compte utilisateur désactivé (400 Bad Request)")
        void shouldFailLoginWhenUserAccountIsDeactivated() throws Exception {
            Entreprise ent = createEntreprise("INACTIVE_ENT", "Inactive Entreprise");
            User user = createUser("inactive@corp.com", "Pass12345!", Role.DESIGNER, ent);
            user.setActif(false);
            userRepository.save(user);

            LoginRequest request = LoginRequest.builder()
                    .email("inactive@corp.com")
                    .motDePasse("Pass12345!")
                    .build();

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Ce compte utilisateur est")));
        }
    }
}
