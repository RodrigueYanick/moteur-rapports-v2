package com.rapports.moteur.service;

import com.rapports.moteur.config.AppProperties;
import com.rapports.moteur.dto.dtoAuth.EnterpriseUpgradeRequest;
import com.rapports.moteur.dto.dtoAuth.LoginResponse;
import com.rapports.moteur.entity.AccountType;
import com.rapports.moteur.entity.Entreprise;
import com.rapports.moteur.entity.Role;
import com.rapports.moteur.entity.User;
import com.rapports.moteur.repository.DocumentRepository;
import com.rapports.moteur.repository.EntrepriseRepository;
import com.rapports.moteur.repository.ReportTemplateRepository;
import com.rapports.moteur.repository.UserRepository;
import com.rapports.moteur.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntrepriseUpgradeTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EntrepriseRepository entrepriseRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ReportTemplateRepository templateRepository;

    private JwtTokenProvider tokenProvider;
    private EntrepriseService entrepriseService;

    private User personalUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getJwt().setSecret("0123456789012345678901234567890123456789012345678901234567890123");
        appProperties.getJwt().setExpirationMs(3600000);
        tokenProvider = new JwtTokenProvider(appProperties);

        entrepriseService = new EntrepriseService(
                request,
                userRepository,
                entrepriseRepository,
                documentRepository,
                templateRepository,
                tokenProvider
        );

        userId = UUID.randomUUID();
        personalUser = User.builder()
                .id(userId)
                .email("etudiant@univ.fr")
                .nom("Dupont")
                .prenom("Jean")
                .motDePasse("hash123")
                .role(Role.DESIGNER)
                .accountType(AccountType.PERSONAL)
                .actif(true)
                .build();
    }

    @Test
    @DisplayName("Upgrade personnel vers entreprise avec nouveau code généré : met à jour le rôle, l'entreprise et migre les documents")
    void testUpgradeToEnterprise_GeneratesCodeAndMigratesDocuments() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(personalUser));
        when(entrepriseRepository.existsByCode(anyString())).thenReturn(false);
        when(entrepriseRepository.save(any(Entreprise.class))).thenAnswer(inv -> {
            Entreprise e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        EnterpriseUpgradeRequest upgradeRequest = EnterpriseUpgradeRequest.builder()
                .nomEntreprise("Acme Corp")
                .emailContact("contact@acmecorp.com")
                .siret("12345678900012")
                .build();

        LoginResponse response = entrepriseService.upgradeToEnterprise(userId, upgradeRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getAccountType()).isEqualTo(AccountType.ENTERPRISE);
        assertThat(response.getUser().getRole()).isEqualTo(Role.ADMIN_ENTREPRISE);
        assertThat(response.getUser().getCodeEntreprise()).isNotNull().startsWith("ENT-");

        // Vérifier la migration des documents et templates
        String oldPersonalCode = "USER-" + userId;
        verify(documentRepository).updateCodeEntreprise(eq(oldPersonalCode), eq(response.getUser().getCodeEntreprise()));
        verify(templateRepository).updateCodeEntreprise(eq(oldPersonalCode), eq(response.getUser().getCodeEntreprise()));
    }

    @Test
    @DisplayName("Upgrade personnel avec code entreprise explicite existant : associe à l'entreprise existante")
    void testUpgradeToEnterprise_ExistingCode() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(personalUser));

        Entreprise existingEntreprise = Entreprise.builder()
                .id(UUID.randomUUID())
                .code("CORP-999")
                .nom("Existing Holding")
                .actif(true)
                .build();
        when(entrepriseRepository.findByCode("CORP-999")).thenReturn(Optional.of(existingEntreprise));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        EnterpriseUpgradeRequest upgradeRequest = EnterpriseUpgradeRequest.builder()
                .nomEntreprise("Holding")
                .codeEntreprise("CORP-999")
                .build();

        LoginResponse response = entrepriseService.upgradeToEnterprise(userId, upgradeRequest);

        assertThat(response.getUser().getCodeEntreprise()).isEqualTo("CORP-999");
        assertThat(response.getUser().getRole()).isEqualTo(Role.ADMIN_ENTREPRISE);
        verify(documentRepository).updateCodeEntreprise(eq("USER-" + userId), eq("CORP-999"));
    }
}

