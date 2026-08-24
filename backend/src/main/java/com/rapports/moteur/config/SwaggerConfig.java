package com.rapports.moteur.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String HEADER_NAME = "X-Entreprise-Code";
    private static final String SECURITY_SCHEME_NAME = "EntrepriseCode";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Moteur de Rapports – API")
                        .description("""
                            Bienvenue sur l'API du Moteur de Rapports Dynamiques.
                            
                            ## Fonctionnalités
                            - Création et gestion de modèles de documents (templates).
                            - Gestion des variables et du schéma.
                            - Génération de PDF et d'aperçus HTML.
                            - Isolation multi-entreprise via le header `X-Entreprise-Code`.
                            
                            ## Utilisation rapide
                            1. Choisissez un endpoint ci-dessous.
                            2. Cliquez sur **Try it out**.
                            3. Remplissez les paramètres, notamment le header `X-Entreprise-Code` si nécessaire.
                            4. Cliquez sur **Execute**.
                        """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Équipe de développement")
                                .email("rodrigueyanickpro@gmail.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                                .tags(List.of(
                                    new Tag().name("Modèles").description("Création, publication et gestion des modèles")
                                //     new Tag().name("Variables").description("Gestion des variables d'un modèle"),
                                //     new Tag().name("Génération").description("Génération de PDF et d'aperçus HTML"),
                                //     new Tag().name("Documents").description("Gestion des documents générés"),
                                //     new Tag().name("Historique").description("Consultation de l'historique des générations")
                                ))
                                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name(HEADER_NAME)
                                        .description("Code entreprise (optionnel, laisser vide pour les templates publics)")))
                // Applique le schéma de sécurité à toutes les opérations
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
    

}