package com.rapports.moteur.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests Unitaires : MockAiClient (Génération déterministe hors-ligne)")
class MockAiClientTest {

    private MockAiClient mockAiClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockAiClient = new MockAiClient();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("MockAiClient est toujours disponible et a pour provider 'mock'")
    void testAvailabilityAndProvider() {
        assertThat(mockAiClient.isAvailable()).isTrue();
        assertThat(mockAiClient.getProviderName()).isEqualTo("mock");
    }

    @Test
    @DisplayName("Génération de modèle JSON valide pour un devis")
    void testGenerateTemplateJsonForDevis() throws Exception {
        String result = mockAiClient.generateContent(
                "System prompt for template builder",
                "Génère un devis artisan avec tableau",
                true
        );

        assertThat(result).isNotBlank();
        JsonNode root = objectMapper.readTree(result);

        assertThat(root.has("pages")).isTrue();
        JsonNode pages = root.get("pages");
        assertThat(pages.isArray()).isTrue();
        assertThat(pages.size()).isGreaterThanOrEqualTo(1);

        JsonNode blocs = pages.get(0).get("blocs");
        assertThat(blocs.isArray()).isTrue();
        assertThat(blocs.size()).isGreaterThan(3);

        // Vérifier la présence d'un titre, texte et tableau
        boolean hasTitre = false;
        boolean hasTableau = false;
        for (JsonNode bloc : blocs) {
            String type = bloc.path("type").asText();
            if ("titre".equals(type)) hasTitre = true;
            if ("tableau".equals(type)) hasTableau = true;
        }

        assertThat(hasTitre).isTrue();
        assertThat(hasTableau).isTrue();
    }

    @Test
    @DisplayName("Génération de données de test JSON réalistes (Mock Data)")
    void testGenerateMockDataJson() throws Exception {
        String result = mockAiClient.generateContent(
                "System instruction for mock data generator",
                "Variables: numero_document, total_ht, lignes",
                true
        );

        assertThat(result).isNotBlank();
        JsonNode root = objectMapper.readTree(result);

        assertThat(root.has("numero_document")).isTrue();
        assertThat(root.has("total_ht")).isTrue();
        assertThat(root.has("total_ttc")).isTrue();
        assertThat(root.has("lignes")).isTrue();

        JsonNode lignes = root.get("lignes");
        assertThat(lignes.isArray()).isTrue();
        assertThat(lignes.size()).isGreaterThanOrEqualTo(2);

        JsonNode firstLine = lignes.get(0);
        assertThat(firstLine.has("designation")).isTrue();
        assertThat(firstLine.has("quantite")).isTrue();
        assertThat(firstLine.has("prix_unitaire")).isTrue();
        assertThat(firstLine.has("total_ligne")).isTrue();
    }
}

