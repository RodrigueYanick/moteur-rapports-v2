package com.rapports.moteur.config;

import com.rapports.moteur.entity.Entreprise;
import com.rapports.moteur.entity.Role;
import com.rapports.moteur.entity.User;
import com.rapports.moteur.repository.EntrepriseRepository;
import com.rapports.moteur.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final EntrepriseRepository entrepriseRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 1. Initialisation de l'Entreprise Démo
        Entreprise entrepriseDemo = entrepriseRepository.findByCode("ENT-001").orElseGet(() -> {
            log.info("Création de l'entreprise par défaut 'ENT-001'...");
            return entrepriseRepository.save(
                    Entreprise.builder()
                            .code("ENT-001")
                            .nom("Entreprise Démo")
                            .emailContact("contact@demo.com")
                            .actif(true)
                            .build()
            );
        });

        // 2. Initialisation de l'administrateur démo
        if (!userRepository.existsByEmail("admin@rapports.com")) {
            log.info("Création du compte administrateur démo admin@rapports.com / admin123 ...");
            userRepository.save(
                    User.builder()
                            .email("admin@rapports.com")
                            .motDePasse(passwordEncoder.encode("admin123"))
                            .nom("Dupont")
                            .prenom("Jean")
                            .role(Role.ADMIN_ENTREPRISE)
                            .entreprise(entrepriseDemo)
                            .actif(true)
                            .build()
            );
        }

        // 3. Initialisation du compte designer démo
        if (!userRepository.existsByEmail("designer@rapports.com")) {
            log.info("Création du compte designer démo designer@rapports.com / designer123 ...");
            userRepository.save(
                    User.builder()
                            .email("designer@rapports.com")
                            .motDePasse(passwordEncoder.encode("designer123"))
                            .nom("Martin")
                            .prenom("Sophie")
                            .role(Role.DESIGNER)
                            .entreprise(entrepriseDemo)
                            .actif(true)
                            .build()
            );
        }
    }
}

