package com.rapports.moteur.repository;

import com.rapports.moteur.entity.DataSourceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DataSourceConfigRepository extends JpaRepository<DataSourceConfig, UUID> {

    List<DataSourceConfig> findByCodeEntrepriseOrderByNomAsc(String codeEntreprise);

    Optional<DataSourceConfig> findByIdAndCodeEntreprise(UUID id, String codeEntreprise);

    boolean existsByCodeEntrepriseAndNom(String codeEntreprise, String nom);
}
