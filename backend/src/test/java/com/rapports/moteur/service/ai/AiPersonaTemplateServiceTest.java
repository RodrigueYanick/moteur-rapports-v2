package com.rapports.moteur.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.dto.dtoAi.AiPersonaGenerationResponse;
import com.rapports.moteur.dto.dtoAi.AiPersonaPromptRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("Tests unitaires du service IA Multi-Persona (AiPersonaTemplateService)")
class AiPersonaTemplateServiceTest {

    @Mock
    private AiClientManager aiClientManager;

    private ObjectMapper objectMapper;
    private AiPersonaTemplateService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        when(aiClientManager.isAiEnabled()).thenReturn(false);
        service = new AiPersonaTemplateService(aiClientManager, objectMapper);
    }

    @Test
    @DisplayName("Génération CV Canadien : respect strict de la non-discrimination (aucune photo, format Letter)")
    void testGeneratePersonaTemplate_CvCanadian() {
        AiPersonaPromptRequest request = AiPersonaPromptRequest.builder()
                .personaType("CV_CANADIAN")
                .userPrompt("Ingénieur cloud AWS à Montréal avec 5 ans d'expérience")
                .targetLanguage("fr")
                .build();

        AiPersonaGenerationResponse response = service.generatePersonaTemplate(request);

        assertThat(response).isNotNull();
        assertThat(response.getPersonaType()).isEqualTo("CV_CANADIAN");
        assertThat(response.getFormatPapier()).isEqualTo("Letter");
        assertThat(response.getCategorie()).isEqualTo("CV_CANADIAN");
        assertThat(response.getContenuDesign()).isNotBlank();
        // Vérification de l'absence totale de bloc photo ou discriminant
        assertThat(response.getContenuDesign().toLowerCase()).doesNotContain("avatar");
        assertThat(response.getContenuDesign().toLowerCase()).doesNotContain("marié");
        assertThat(response.getSampleData()).isNotEmpty();
    }

    @Test
    @DisplayName("Génération CV ATS : format 100% linéaire et format Letter")
    void testGeneratePersonaTemplate_CvAts() {
        AiPersonaPromptRequest request = AiPersonaPromptRequest.builder()
                .personaType("CV_ATS")
                .userPrompt("Senior Fullstack Developer (Java, Angular, Docker)")
                .targetLanguage("fr")
                .build();

        AiPersonaGenerationResponse response = service.generatePersonaTemplate(request);

        assertThat(response).isNotNull();
        assertThat(response.getPersonaType()).isEqualTo("CV_ATS");
        assertThat(response.getFormatPapier()).isEqualTo("Letter");
        assertThat(response.getCategorie()).isEqualTo("CV_ATS");
        assertThat(response.getContenuDesign()).isNotBlank();
        assertThat(response.getContenuDesign().toLowerCase()).doesNotContain("photo");
    }

    @Test
    @DisplayName("Génération Rapport de Stage / PFE : Sommaire hiérarchique et format A4")
    void testGeneratePersonaTemplate_StageReport() {
        AiPersonaPromptRequest request = AiPersonaPromptRequest.builder()
                .personaType("STAGE_REPORT")
                .userPrompt("Rapport de fin d'études en sécurité informatique chez Sopra Steria")
                .targetLanguage("fr")
                .build();

        AiPersonaGenerationResponse response = service.generatePersonaTemplate(request);

        assertThat(response).isNotNull();
        assertThat(response.getPersonaType()).isEqualTo("STAGE_REPORT");
        assertThat(response.getFormatPapier()).isEqualTo("A4");
        assertThat(response.getCategorie()).isEqualTo("STAGE_ACADEMIQUE");
        assertThat(response.getContenuDesign()).contains("Page de Garde");
        assertThat(response.getSampleData()).containsKey("titre_sujet");
    }

    @Test
    @DisplayName("Génération Facture Freelance : calculs légaux et format A4")
    void testGeneratePersonaTemplate_FreelanceInvoice() {
        AiPersonaPromptRequest request = AiPersonaPromptRequest.builder()
                .personaType("FREELANCE_INVOICE")
                .userPrompt("Prestation de conseil DevOps 10 jours à 650€ / jour")
                .targetLanguage("fr")
                .build();

        AiPersonaGenerationResponse response = service.generatePersonaTemplate(request);

        assertThat(response).isNotNull();
        assertThat(response.getPersonaType()).isEqualTo("FREELANCE_INVOICE");
        assertThat(response.getFormatPapier()).isEqualTo("A4");
        assertThat(response.getCategorie()).isEqualTo("FACTURE_DEVIS");
        assertThat(response.getContenuDesign()).contains("Total HT");
        assertThat(response.getSampleData()).containsKey("numero_facture");
        assertThat(response.getSampleData()).containsKey("total_ht");
    }

    @Test
    @DisplayName("Enforce constraints supprime effectivement les photos injectées dans un CV canadien")
    void testEnforcePersonaConstraints_StripsPhoto() throws Exception {
        String designWithPhoto = """
            {
              "pages": [
                {
                  "nom": "Page 1",
                  "blocs": [
                    { "id": "photo-profil", "type": "image", "contenu": "http://img.com/me.jpg" },
                    { "id": "nom-titre", "type": "texte", "contenu": "Jean Dupont" }
                  ]
                }
              ]
            }
            """;

        AiPersonaGenerationResponse response = AiPersonaGenerationResponse.builder()
                .personaType("CV_CANADIAN")
                .contenuDesign(designWithPhoto)
                .build();

        service.enforcePersonaConstraints(response, "CV_CANADIAN");

        assertThat(response.getFormatPapier()).isEqualTo("Letter");
        assertThat(response.getContenuDesign()).doesNotContain("photo-profil");
        assertThat(response.getContenuDesign()).contains("nom-titre");
    }
}
