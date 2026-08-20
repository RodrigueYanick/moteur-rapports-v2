package com.rapports.moteur.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Moteur de Rapports – API")
                        .description("""
                                API pour la création de modèles de documents,
                                la gestion des variables et la génération de rapports PDF.
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Équipe de développement")
                                .email("dev@entreprise.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}