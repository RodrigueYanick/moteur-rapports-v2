package com.rapports.moteur.controller;

import com.rapports.moteur.entity.Categorie;
import com.rapports.moteur.entity.PaginationMode;
import com.rapports.moteur.entity.ReportTemplate;
import com.rapports.moteur.entity.TemplateStatus;
import com.rapports.moteur.repository.ReportTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Tests du contrôleur PublicPortalController (Accès anonyme et vitrine de modèles)")
class PublicPortalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReportTemplateRepository templateRepository;

    private ReportTemplate templatePublicCv;
    private ReportTemplate templatePublicStage;

    @BeforeEach
    void setUp() {
        templatePublicCv = templateRepository.save(
                ReportTemplate.builder()
                        .nom("CV Public Test Mock")
                        .description("Description test")
                        .categorie(Categorie.CV_CANADIAN)
                        .formatPapier("Letter")
                        .modePagination(PaginationMode.FIXED)
                        .statut(TemplateStatus.PUBLIE)
                        .version(1)
                        .codeEntreprise(null)
                        .contenuDesign("{\"pages\":[{\"nom\":\"P1\",\"blocs\":[]}]}")
                        .schema("{\"type\":\"object\"}")
                        .dateCreation(LocalDateTime.now())
                        .dateModification(LocalDateTime.now())
                        .build()
        );

        templatePublicStage = templateRepository.save(
                ReportTemplate.builder()
                        .nom("Stage Public Test Mock")
                        .description("Description stage")
                        .categorie(Categorie.STAGE_ACADEMIQUE)
                        .formatPapier("A4")
                        .modePagination(PaginationMode.FIXED)
                        .statut(TemplateStatus.PUBLIE)
                        .version(1)
                        .codeEntreprise(null)
                        .contenuDesign("{\"pages\":[{\"nom\":\"P1\",\"blocs\":[]}]}")
                        .schema("{\"type\":\"object\"}")
                        .dateCreation(LocalDateTime.now())
                        .dateModification(LocalDateTime.now())
                        .build()
        );
    }

    @Test
    @DisplayName("GET /api/public/categories accessible sans token et retourne les catégories officielles")
    void getPublicCategories_SuccessWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/public/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(5))))
                .andExpect(jsonPath("$[*].code", hasItem("CV_CANADIAN")))
                .andExpect(jsonPath("$[*].code", hasItem("CV_ATS")))
                .andExpect(jsonPath("$[*].code", hasItem("STAGE_ACADEMIQUE")))
                .andExpect(jsonPath("$[*].code", hasItem("FACTURE_DEVIS")));
    }

    @Test
    @DisplayName("GET /api/public/templates accessible sans authentification et renvoie les modèles publics")
    void getPublicTemplates_SuccessWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/public/templates")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].nom", hasItem("CV Public Test Mock")));
    }

    @Test
    @DisplayName("GET /api/public/templates avec filtre de catégorie")
    void getPublicTemplates_WithCategoryFilter() throws Exception {
        mockMvc.perform(get("/api/public/templates")
                        .param("categorie", "CV_CANADIAN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nom", hasItem("CV Public Test Mock")))
                .andExpect(jsonPath("$[*].nom", not(hasItem("Stage Public Test Mock"))));
    }

    @Test
    @DisplayName("GET /api/public/templates/{id} renvoie le modèle public par son ID")
    void getPublicTemplateById_Success() throws Exception {
        mockMvc.perform(get("/api/public/templates/{id}", templatePublicCv.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(templatePublicCv.getId().toString())))
                .andExpect(jsonPath("$.nom", is("CV Public Test Mock")))
                .andExpect(jsonPath("$.codeEntreprise").doesNotExist());
    }
}

