package com.rapports.moteur.service;

import com.rapports.moteur.config.AppProperties;
import com.rapports.moteur.dto.dtoAuth.LoginRequest;
import com.rapports.moteur.dto.dtoAuth.LoginResponse;
import com.rapports.moteur.dto.dtoAuth.RegisterRequest;
import com.rapports.moteur.entity.Entreprise;
import com.rapports.moteur.entity.Role;
import com.rapports.moteur.entity.User;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.repository.EntrepriseRepository;
import com.rapports.moteur.repository.UserRepository;
import com.rapports.moteur.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntrepriseRepository entrepriseRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtTokenProvider tokenProvider;
    private AuthService authService;

    private Entreprise testEntreprise;
    private User testUser;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setSecret("0123456789012345678901234567890123456789012345678901234567890123");
        appProperties.getJwt().setExpirationMs(3600000);
        tokenProvider = new JwtTokenProvider(appProperties);

        authService = new AuthService(userRepository, entrepriseRepository, passwordEncoder, tokenProvider);

        testEntreprise = Entreprise.builder()
                .id(UUID.randomUUID())
                .code("ENT-001")
                .nom("Entreprise Test")
                .actif(true)
                .build();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .motDePasse("hashed_password")
                .nom("Test")
                .prenom("User")
                .role(Role.ADMIN_ENTREPRISE)
                .entreprise(testEntreprise)
                .actif(true)
                .build();
    }

    @Test
    @DisplayName("Connexion réussie avec des identifiants valides")
    void login_Success() {
        LoginRequest request = new LoginRequest("test@example.com", "secret123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("secret123", "hashed_password")).thenReturn(true);

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getUser().getCodeEntreprise()).isEqualTo("ENT-001");
        assertThat(response.getUser().getRole()).isEqualTo(Role.ADMIN_ENTREPRISE);
        assertThat(tokenProvider.validateToken(response.getToken())).isTrue();
    }

    @Test
    @DisplayName("Échec de connexion si le mot de passe est invalide")
    void login_InvalidPassword_ThrowsException() {
        LoginRequest request = new LoginRequest("test@example.com", "wrong_password");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Échec de connexion si l'utilisateur est introuvable")
    void login_UserNotFound_ThrowsException() {
        LoginRequest request = new LoginRequest("inconnu@example.com", "password");

        when(userRepository.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("Inscription réussie créant l'utilisateur et son entreprise")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .email("nouveau@example.com")
                .motDePasse("pass123")
                .nom("Nouveau")
                .prenom("Client")
                .nomEntreprise("Acme Corp")
                .build();

        when(userRepository.existsByEmail("nouveau@example.com")).thenReturn(false);
        when(entrepriseRepository.save(any(Entreprise.class))).thenAnswer(invocation -> {
            Entreprise e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(passwordEncoder.encode("pass123")).thenReturn("encoded_pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        LoginResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUser().getEmail()).isEqualTo("nouveau@example.com");
        assertThat(response.getUser().getNomEntreprise()).isEqualTo("Acme Corp");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Échec d'inscription si l'email existe déjà")
    void register_ExistingEmail_ThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .motDePasse("pass123")
                .nom("Test")
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ValidationException.class);
    }
}

