package com.rapports.moteur;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.rapports.moteur.entity")
@EnableJpaRepositories(basePackages = "com.rapports.moteur.repository")
public class MoteurRapportsApplication {
    public static void main(String[] args) {
        SpringApplication.run(MoteurRapportsApplication.class, args);
    }
}
