package com.rapports.moteur.service;

import com.rapports.moteur.dto.datasource.*;
import com.rapports.moteur.entity.DataSourceConfig;
import com.rapports.moteur.exceptions.ValidationException;
import com.rapports.moteur.mapper.DataSourceMapper;
import com.rapports.moteur.repository.DataSourceConfigRepository;
import com.rapports.moteur.security.crypto.AesCryptoService;
import com.rapports.moteur.service.datasource.DataSourceExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataSourceService {

    private final DataSourceConfigRepository repository;
    private final DataSourceMapper mapper;
    private final DataSourceExecutionService executionService;
    private final EntrepriseService entrepriseService;
    private final AesCryptoService cryptoService;

    public List<DataSourceResponse> listDataSources() {
        String codeEntreprise = entrepriseService.getCurrentCodeEntreprise();
        if (codeEntreprise == null || codeEntreprise.isBlank()) {
            return Collections.emptyList();
        }

        return repository.findByCodeEntrepriseOrderByNomAsc(codeEntreprise).stream()
                .map(mapper::toResponse)
                .toList();
    }

    public DataSourceResponse getDataSource(UUID id) {
        return mapper.toResponse(findAndVerify(id));
    }

    @Transactional
    public DataSourceResponse createDataSource(DataSourceRequest request) {
        String codeEntreprise = entrepriseService.getCurrentCodeEntreprise();
        if (codeEntreprise == null || codeEntreprise.isBlank()) {
            throw new ValidationException("Code entreprise manquant dans la session");
        }

        if (repository.existsByCodeEntrepriseAndNom(codeEntreprise, request.getNom())) {
            throw new ValidationException("Une source de données avec ce nom existe déjà pour cette entreprise");
        }

        String motDePasseChiffre = (request.getMotDePasse() != null && !request.getMotDePasse().isBlank())
                ? cryptoService.encrypt(request.getMotDePasse().trim()) : null;

        DataSourceConfig entity = mapper.toEntity(request, codeEntreprise, motDePasseChiffre);
        DataSourceConfig saved = repository.save(entity);
        log.info("Création de la source de données '{}' (type: {}) pour l'entreprise {}",
                saved.getNom(), saved.getType(), codeEntreprise);

        return mapper.toResponse(saved);
    }

    @Transactional
    public DataSourceResponse updateDataSource(UUID id, DataSourceRequest request) {
        DataSourceConfig entity = findAndVerify(id);
        String codeEntreprise = entity.getCodeEntreprise();

        if (!entity.getNom().equalsIgnoreCase(request.getNom()) &&
                repository.existsByCodeEntrepriseAndNom(codeEntreprise, request.getNom())) {
            throw new ValidationException("Une source de données avec ce nom existe déjà pour cette entreprise");
        }

        entity.setNom(request.getNom());
        entity.setType(request.getType());
        entity.setUrlOuHote(request.getUrlOuHote());
        entity.setPort(request.getPort());
        entity.setNomBase(request.getNomBase());
        entity.setNomUtilisateur(request.getNomUtilisateur());
        entity.setEnTetesJson(request.getEnTetesJson());
        entity.setMethodeHttp(request.getMethodeHttp());
        entity.setAuthType(request.getAuthType());
        entity.setApiKeyHeader(request.getApiKeyHeader());
        entity.setTimeoutSecondes(request.getTimeoutSecondes());
        entity.setActif(request.getActif() != null ? request.getActif() : true);

        // Mise à jour du secret uniquement s'il est explicitement renseigné
        if (request.getMotDePasse() != null && !request.getMotDePasse().isBlank()) {
            entity.setMotDePasseChiffre(cryptoService.encrypt(request.getMotDePasse().trim()));
        }

        DataSourceConfig updated = repository.save(entity);
        log.info("Mise à jour de la source de données '{}' ({})", updated.getNom(), updated.getId());

        return mapper.toResponse(updated);
    }

    @Transactional
    public void deleteDataSource(UUID id) {
        DataSourceConfig entity = findAndVerify(id);
        repository.delete(entity);
        log.info("Suppression de la source de données '{}' ({})", entity.getNom(), id);
    }

    /**
     * Teste la connectivité vers une source (existante ou à la volée).
     */
    public DataSourceTestResult testConnection(DataSourceTestRequest request) {
        long start = System.currentTimeMillis();
        DataSourceConfig config;

        try {
            if (request.getDataSourceId() != null) {
                config = findAndVerify(request.getDataSourceId());
            } else {
                String codeEntreprise = entrepriseService.getCurrentCodeEntreprise();
                String motDePasseChiffre = (request.getMotDePasse() != null && !request.getMotDePasse().isBlank())
                        ? cryptoService.encrypt(request.getMotDePasse().trim()) : null;

                config = DataSourceConfig.builder()
                        .codeEntreprise(codeEntreprise != null ? codeEntreprise : "DEFAULT")
                        .nom("Test Ephemeral")
                        .type(request.getType())
                        .urlOuHote(request.getUrlOuHote())
                        .port(request.getPort())
                        .nomBase(request.getNomBase())
                        .nomUtilisateur(request.getNomUtilisateur())
                        .motDePasseChiffre(motDePasseChiffre)
                        .enTetesJson(request.getEnTetesJson())
                        .methodeHttp(request.getMethodeHttp())
                        .authType(request.getAuthType())
                        .apiKeyHeader(request.getApiKeyHeader())
                        .timeoutSecondes(request.getTimeoutSecondes() != null ? request.getTimeoutSecondes() : 5)
                        .build();
            }

            boolean connected = executionService.testConnection(config, request.getTestQuery());
            long duration = System.currentTimeMillis() - start;

            if (connected) {
                return DataSourceTestResult.builder()
                        .succes(true)
                        .message("Connexion établie avec succès avec la source de données")
                        .tempsReponseMs(duration)
                        .build();
            } else {
                return DataSourceTestResult.builder()
                        .succes(false)
                        .message("Impossible d'établir la connexion (timeout ou rejet)")
                        .tempsReponseMs(duration)
                        .build();
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.warn("Échec du test de connexion : {}", e.getMessage());
            return DataSourceTestResult.builder()
                    .succes(false)
                    .message(e.getMessage() != null ? e.getMessage() : "Échec de connexion")
                    .tempsReponseMs(duration)
                    .build();
        }
    }

    /**
     * Exécute une requête et retourne les données d'aperçu.
     */
    public Object executeQuery(UUID id, DataSourceExecuteRequest request) {
        DataSourceConfig config = findAndVerify(id);
        int maxRows = (request.getMaxRows() != null && request.getMaxRows() > 0) ? request.getMaxRows() : 100;
        return executionService.execute(config, request.getQuery(), request.getParametres(), maxRows);
    }

    public DataSourceConfig findAndVerify(UUID id) {
        String codeEntreprise = entrepriseService.getCurrentCodeEntreprise();
        if (codeEntreprise == null || codeEntreprise.isBlank()) {
            throw new ValidationException("Code entreprise manquant dans la session");
        }

        return repository.findByIdAndCodeEntreprise(id, codeEntreprise)
                .orElseThrow(() -> new ValidationException("Source de données introuvable pour l'identifiant : " + id));
    }
}
