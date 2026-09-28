package com.rapports.moteur.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "data_source_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataSourceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code_entreprise", nullable = false, length = 50)
    private String codeEntreprise;

    @Column(nullable = false, length = 150)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DataSourceType type;

    @Column(name = "url_ou_hote", nullable = false, length = 500)
    private String urlOuHote;

    private Integer port;

    @Column(name = "nom_base", length = 100)
    private String nomBase;

    @Column(name = "nom_utilisateur", length = 150)
    private String nomUtilisateur;

    @Column(name = "mot_de_passe_chiffre", columnDefinition = "TEXT")
    private String motDePasseChiffre;

    @Column(name = "en_tetes_json", columnDefinition = "TEXT")
    private String enTetesJson;

    @Column(name = "methode_http", length = 10)
    @Builder.Default
    private String methodeHttp = "GET";

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", length = 30)
    @Builder.Default
    private DataSourceAuthType authType = DataSourceAuthType.NONE;

    @Column(name = "api_key_header", length = 100)
    private String apiKeyHeader;

    @Column(name = "timeout_secondes")
    @Builder.Default
    private Integer timeoutSecondes = 10;

    @Column(nullable = false)
    @Builder.Default
    private Boolean actif = true;

    @CreationTimestamp
    @Column(name = "date_creation", updatable = false)
    private LocalDateTime dateCreation;

    @UpdateTimestamp
    @Column(name = "date_modification")
    private LocalDateTime dateModification;
}
