package com.rapports.moteur.service;

import com.rapports.moteur.dto.datasource.DataSourceRequest;
import com.rapports.moteur.dto.datasource.DataSourceResponse;
import com.rapports.moteur.dto.datasource.DataSourceTestRequest;
import com.rapports.moteur.dto.datasource.DataSourceTestResult;
import com.rapports.moteur.entity.DataSourceAuthType;
import com.rapports.moteur.entity.DataSourceConfig;
import com.rapports.moteur.entity.DataSourceType;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.mapper.DataSourceMapper;
import com.rapports.moteur.repository.DataSourceConfigRepository;
import com.rapports.moteur.security.crypto.AesCryptoService;
import com.rapports.moteur.service.datasource.DataSourceExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSourceServiceTest {

    @Mock
    private DataSourceConfigRepository repository;

    @Mock
    private DataSourceExecutionService executionService;

    @Mock
    private EntrepriseService entrepriseService;

    @Mock
    private AesCryptoService cryptoService;

    private DataSourceMapper mapper = new DataSourceMapper();

    private DataSourceService dataSourceService;

    private final String CODE_ENTREPRISE = "ENT-001";
    private final UUID DS_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        dataSourceService = new DataSourceService(
                repository,
                mapper,
                executionService,
                entrepriseService,
                cryptoService
        );
        lenient().when(entrepriseService.getCurrentCodeEntreprise()).thenReturn(CODE_ENTREPRISE);
    }

    @Test
    @DisplayName("Création d'une source SQL avec chiffrement du mot de passe")
    void testCreateDataSourceSuccess() {
        DataSourceRequest request = DataSourceRequest.builder()
                .nom("Postgres ERP")
                .type(DataSourceType.POSTGRESQL)
                .urlOuHote("postgres.mon-entreprise.com")
                .port(5432)
                .nomBase("erp_prod")
                .nomUtilisateur("readonly_user")
                .motDePasse("SuperSecret123!")
                .build();

        when(repository.existsByCodeEntrepriseAndNom(CODE_ENTREPRISE, "Postgres ERP")).thenReturn(false);
        when(cryptoService.encrypt("SuperSecret123!")).thenReturn("ENC_BASE64_PAYLOAD");

        DataSourceConfig savedEntity = mapper.toEntity(request, CODE_ENTREPRISE, "ENC_BASE64_PAYLOAD");
        savedEntity.setId(DS_ID);

        when(repository.save(any(DataSourceConfig.class))).thenReturn(savedEntity);

        DataSourceResponse response = dataSourceService.createDataSource(request);

        assertNotNull(response);
        assertEquals("Postgres ERP", response.getNom());
        assertEquals(DataSourceType.POSTGRESQL, response.getType());
        assertTrue(response.isAMotDePasse(), "Le mot de passe doit être marqué comme présent");
        verify(cryptoService, times(1)).encrypt("SuperSecret123!");
        verify(repository, times(1)).save(any(DataSourceConfig.class));
    }

    @Test
    @DisplayName("Rejet de création en cas de doublon de nom dans la même entreprise")
    void testCreateDataSourceDuplicateName() {
        DataSourceRequest request = DataSourceRequest.builder()
                .nom("Doublon Source")
                .type(DataSourceType.MYSQL)
                .urlOuHote("localhost")
                .build();

        when(repository.existsByCodeEntrepriseAndNom(CODE_ENTREPRISE, "Doublon Source")).thenReturn(true);

        ValidationException ex = assertThrows(ValidationException.class, () -> dataSourceService.createDataSource(request));
        assertTrue(ex.getMessage().contains("existe déjà"));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Récupération d'une source existante de l'entreprise")
    void testGetDataSourceSuccess() {
        DataSourceConfig entity = DataSourceConfig.builder()
                .id(DS_ID)
                .codeEntreprise(CODE_ENTREPRISE)
                .nom("API Facturation")
                .type(DataSourceType.REST_API)
                .urlOuHote("https://api.erp.com")
                .authType(DataSourceAuthType.BEARER)
                .motDePasseChiffre("ENC_TOKEN")
                .build();

        when(repository.findByIdAndCodeEntreprise(DS_ID, CODE_ENTREPRISE)).thenReturn(Optional.of(entity));

        DataSourceResponse response = dataSourceService.getDataSource(DS_ID);

        assertNotNull(response);
        assertEquals("API Facturation", response.getNom());
        assertEquals(DataSourceType.REST_API, response.getType());
        assertTrue(response.isAMotDePasse());
    }

    @Test
    @DisplayName("Rejet si la source appartient à une autre entreprise (Isolation Multi-Tenant)")
    void testGetDataSourceNotFoundForOtherEnterprise() {
        when(repository.findByIdAndCodeEntreprise(DS_ID, CODE_ENTREPRISE)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> dataSourceService.getDataSource(DS_ID));
    }

    @Test
    @DisplayName("Test de connectivité réussi (ping distant)")
    void testTestConnectionSuccess() {
        DataSourceTestRequest testReq = DataSourceTestRequest.builder()
                .type(DataSourceType.POSTGRESQL)
                .urlOuHote("192.168.1.50")
                .nomBase("testdb")
                .nomUtilisateur("user")
                .motDePasse("pwd")
                .build();

        when(executionService.testConnection(any(DataSourceConfig.class), isNull())).thenReturn(true);

        DataSourceTestResult result = dataSourceService.testConnection(testReq);

        assertTrue(result.isSucces());
        assertTrue(result.getMessage().contains("succès"));
    }

    @Test
    @DisplayName("Test de connectivité avec échec retourné")
    void testTestConnectionFailure() {
        DataSourceTestRequest testReq = DataSourceTestRequest.builder()
                .type(DataSourceType.REST_API)
                .urlOuHote("https://invalid-host.example.com")
                .build();

        when(executionService.testConnection(any(DataSourceConfig.class), isNull()))
                .thenThrow(new RuntimeException("Host unreachable"));

        DataSourceTestResult result = dataSourceService.testConnection(testReq);

        assertFalse(result.isSucces());
        assertTrue(result.getMessage().contains("Host unreachable"));
    }
}
