package com.rapports.moteur.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rapports.moteur.entity.*;
import com.rapports.moteur.repository.*;
import com.rapports.moteur.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected EntrepriseRepository entrepriseRepository;

    @Autowired
    protected ReportTemplateRepository templateRepository;

    @Autowired
    protected ReportVariableRepository variableRepository;

    @Autowired
    protected ReportGenerationRepository generationRepository;

    @Autowired
    protected DocumentRepository documentRepository;

    @Autowired
    protected ReportBatchRepository batchRepository;

    @Autowired
    protected BatchGenerationItemRepository batchItemRepository;

    @Autowired
    protected CompanyWorkspaceConfigRepository workspaceConfigRepository;

    @BeforeEach
    void baseSetUp() {
        cleanDatabase();
    }

    protected void cleanDatabase() {
        batchItemRepository.deleteAll();
        batchRepository.deleteAll();
        documentRepository.deleteAll();
        generationRepository.deleteAll();
        variableRepository.deleteAll();

        templateRepository.findAll().forEach(t -> {
            if (t.getParentTemplate() != null) {
                t.setParentTemplate(null);
                templateRepository.save(t);
            }
        });
        templateRepository.deleteAll();

        workspaceConfigRepository.deleteAll();
        userRepository.deleteAll();
        entrepriseRepository.deleteAll();
    }

    protected Entreprise createEntreprise(String code, String nom) {
        Entreprise ent = Entreprise.builder()
                .code(code)
                .nom(nom)
                .emailContact("contact@" + code.toLowerCase() + ".com")
                .actif(true)
                .dateCreation(LocalDateTime.now())
                .build();
        return entrepriseRepository.save(ent);
    }

    protected User createUser(String email, String rawPassword, Role role, Entreprise entreprise) {
        User user = User.builder()
                .email(email)
                .motDePasse(passwordEncoder.encode(rawPassword))
                .nom("Doe")
                .prenom("John")
                .role(role)
                .entreprise(entreprise)
                .actif(true)
                .dateCreation(LocalDateTime.now())
                .build();
        return userRepository.save(user);
    }

    protected String getBearerToken(User user) {
        return "Bearer " + jwtTokenProvider.generateToken(user);
    }

    protected ReportTemplate createTemplate(String nom, String codeEntreprise, TemplateStatus statut) {
        ReportTemplate template = ReportTemplate.builder()
                .nom(nom)
                .description("Description de " + nom)
                .codeEntreprise(codeEntreprise)
                .statut(statut)
                .version(1)
                .formatPapier("A4")
                .modePagination(PaginationMode.FIXED)
                .contenuDesign("{\"pages\":[{\"blocs\":[]}]}")
                .schema("[]")
                .dateCreation(LocalDateTime.now())
                .dateModification(LocalDateTime.now())
                .build();
        return templateRepository.save(template);
    }
}

