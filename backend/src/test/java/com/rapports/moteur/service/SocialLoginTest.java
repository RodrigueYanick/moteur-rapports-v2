package com.rapports.moteur.service;

import com.rapports.moteur.config.AppProperties;
import com.rapports.moteur.dto.dtoAuth.LoginResponse;
import com.rapports.moteur.dto.dtoAuth.SocialLoginRequest;
import com.rapports.moteur.entity.AccountType;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialLoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntrepriseRepository entrepriseRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtTokenProvider tokenProvider;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setSecret("0123456789012345678901234567890123456789012345678901234567890123");
        appProperties.getJwt().setExpirationMs(3600000);
        tokenProvider = new JwtTokenProvider(appProperties);

        authService = new AuthService(userRepository, entrepriseRepository, passwordEncoder, tokenProvider);
    }

    @Test
    @DisplayName("Social login d'un nouvel utilisateur : auto-création en compte personnel et génération JWT")
    void testSocialLogin_NewUserAutoRegistered() {
        when(userRepository.findByEmail("nouveau.google@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pwd");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        SocialLoginRequest request = SocialLoginRequest.builder()
                .provider("GOOGLE")
                .email("nouveau.google@gmail.com")
                .nom("Curie")
                .prenom("Marie")
                .build();

        LoginResponse response = authService.socialLogin(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("nouveau.google@gmail.com");
        assertThat(response.getUser().getAccountType()).isEqualTo(AccountType.PERSONAL);
        assertThat(response.getUser().getRole()).isEqualTo(Role.DESIGNER);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Social login d'un utilisateur existant actif : connexion immédiate sans réinscription")
    void testSocialLogin_ExistingActiveUser() {
        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .email("deja.inscrit@github.com")
                .nom("Lovelace")
                .prenom("Ada")
                .role(Role.DESIGNER)
                .accountType(AccountType.PERSONAL)
                .actif(true)
                .build();

        when(userRepository.findByEmail("deja.inscrit@github.com")).thenReturn(Optional.of(existingUser));

        SocialLoginRequest request = SocialLoginRequest.builder()
                .provider("GITHUB")
                .email("deja.inscrit@github.com")
                .build();

        LoginResponse response = authService.socialLogin(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUser().getEmail()).isEqualTo("deja.inscrit@github.com");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Social login d'un utilisateur désactivé : lève une exception de validation")
    void testSocialLogin_InactiveUserThrowsException() {
        User inactiveUser = User.builder()
                .id(UUID.randomUUID())
                .email("bloque@google.com")
                .actif(false)
                .build();

        when(userRepository.findByEmail("bloque@google.com")).thenReturn(Optional.of(inactiveUser));

        SocialLoginRequest request = SocialLoginRequest.builder()
                .provider("GOOGLE")
                .email("bloque@google.com")
                .build();

        assertThatThrownBy(() -> authService.socialLogin(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("désactivé");
    }
}

