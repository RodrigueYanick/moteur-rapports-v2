package com.rapports.moteur.integration;

import com.rapports.moteur.dto.datasource.DataSourceExecuteRequest;
import com.rapports.moteur.dto.datasource.DataSourceRequest;
import com.rapports.moteur.dto.datasource.DataSourceTestRequest;
import com.rapports.moteur.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DataSourceIntegrationTest extends BaseIntegrationTest {

    private Entreprise entreprise1;
    private Entreprise entreprise2;
    private User admin1;
    private User admin2;
    private String token1;
    private String token2;

    @BeforeEach
    void setUp() {
        entreprise1 = createEntreprise("ENT-001", "Entreprise Alpha");
        entreprise2 = createEntreprise("ENT-002", "Entreprise Beta");

        admin1 = createUser("admin1@alpha.com", "Password123!", Role.ADMIN_ENTREPRISE, entreprise1);
        admin2 = createUser("admin2@beta.com", "Password123!", Role.ADMIN_ENTREPRISE, entreprise2);

        token1 = getBearerToken(admin1);
        token2 = getBearerToken(admin2);
    }

    @Test
    @DisplayName("Création, consultation et liste d'une source de données avec chiffrement")
    void testCreateAndListDataSource() throws Exception {
        DataSourceRequest request = DataSourceRequest.builder()
                .nom("Postgres ERP Principal")
                .type(DataSourceType.POSTGRESQL)
                .urlOuHote("postgres.alpha.com")
                .port(5432)
                .nomBase("erp_db")
                .nomUtilisateur("alpha_ro")
                .motDePasse("SecretDbPassword99!")
                .build();

        // 1. Création
        String responseJson = mockMvc.perform(post("/api/data-sources")
                        .header("Authorization", token1)
                        .header("X-Entreprise-Code", "ENT-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom", is("Postgres ERP Principal")))
                .andExpect(jsonPath("$.type", is("POSTGRESQL")))
                .andExpect(jsonPath("$.amotDePasse", is(true)))
                .andReturn().getResponse().getContentAsString();

        UUID createdId = UUID.fromString(objectMapper.readTree(responseJson).get("id").asText());

        // 2. Vérification que le mot de passe est chiffré en base
        DataSourceConfig inDb = dataSourceConfigRepository.findById(createdId).orElseThrow();
        assertNotEquals("SecretDbPassword99!", inDb.getMotDePasseChiffre(), "Le mot de passe doit être chiffré au repos");

        // 3. Liste
        mockMvc.perform(get("/api/data-sources")
                        .header("Authorization", token1)
                        .header("X-Entreprise-Code", "ENT-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nom", is("Postgres ERP Principal")));
    }

    @Test
    @DisplayName("Mise à jour et suppression d'une source de données")
    void testUpdateAndDeleteDataSource() throws Exception {
        DataSourceConfig config = DataSourceConfig.builder()
                .codeEntreprise("ENT-001")
                .nom("API Ventes")
                .type(DataSourceType.REST_API)
                .urlOuHote("https://api.alpha.com")
                .authType(DataSourceAuthType.BEARER)
                .motDePasseChiffre("ENC_KEY")
                .build();
        config = dataSourceConfigRepository.save(config);

        // Mise à jour du nom
        DataSourceRequest updateReq = DataSourceRequest.builder()
                .nom("API Ventes V2")
                .type(DataSourceType.REST_API)
                .urlOuHote("https://api.alpha.com/v2")
                .authType(DataSourceAuthType.BEARER)
                .build();

        mockMvc.perform(put("/api/data-sources/" + config.getId())
                        .header("Authorization", token1)
                        .header("X-Entreprise-Code", "ENT-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom", is("API Ventes V2")))
                .andExpect(jsonPath("$.urlOuHote", is("https://api.alpha.com/v2")));

        // Suppression
        mockMvc.perform(delete("/api/data-sources/" + config.getId())
                        .header("Authorization", token1)
                        .header("X-Entreprise-Code", "ENT-001"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/data-sources/" + config.getId())
                        .header("Authorization", token1)
                        .header("X-Entreprise-Code", "ENT-001"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Isolation stricte multi-entreprise : Entreprise 2 ne peut pas accéder aux sources de l'Entreprise 1")
    void testMultiTenancyIsolation() throws Exception {
        DataSourceConfig config1 = DataSourceConfig.builder()
                .codeEntreprise("ENT-001")
                .nom("Source Privée Alpha")
                .type(DataSourceType.POSTGRESQL)
                .urlOuHote("private.alpha.internal")
                .build();
        config1 = dataSourceConfigRepository.save(config1);

        // Entreprise 2 tente d'accéder à la source d'Alpha
        mockMvc.perform(get("/api/data-sources/" + config1.getId())
                        .header("Authorization", token2)
                        .header("X-Entreprise-Code", "ENT-002"))
                .andExpect(status().isBadRequest());

        // Entreprise 2 ne la voit pas dans sa liste
        mockMvc.perform(get("/api/data-sources")
                        .header("Authorization", token2)
                        .header("X-Entreprise-Code", "ENT-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Sécurité SQL : rejet des instructions destructives lors de l'exécution")
    void testSqlInjectionRejection() throws Exception {
        DataSourceConfig config = DataSourceConfig.builder()
                .codeEntreprise("ENT-001")
                .nom("Postgres Test")
                .type(DataSourceType.POSTGRESQL)
                .urlOuHote("localhost")
                .build();
        config = dataSourceConfigRepository.save(config);

        DataSourceExecuteRequest execReq = DataSourceExecuteRequest.builder()
                .query("DROP TABLE document; SELECT * FROM user;")
                .build();

        mockMvc.perform(post("/api/data-sources/" + config.getId() + "/execute")
                        .header("Authorization", token1)
                        .header("X-Entreprise-Code", "ENT-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(execReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Sécurité Réseau (SSRF) : rejet des adresses de métadonnées Cloud et réseaux privés")
    void testSsrfProtection() throws Exception {
        DataSourceTestRequest ssrfReq = DataSourceTestRequest.builder()
                .type(DataSourceType.REST_API)
                .urlOuHote("http://169.254.169.254/latest/meta-data")
                .build();

        mockMvc.perform(post("/api/data-sources/test")
                        .header("Authorization", token1)
                        .header("X-Entreprise-Code", "ENT-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ssrfReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.succes", is(false)))
                .andExpect(jsonPath("$.message", containsString("SSRF")));
    }
}
