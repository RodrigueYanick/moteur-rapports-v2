package com.rapports.moteur.repository;

import com.rapports.moteur.entity.CompanyWorkspaceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyWorkspaceConfigRepository extends JpaRepository<CompanyWorkspaceConfig, UUID> {

    Optional<CompanyWorkspaceConfig> findByCodeEntreprise(String codeEntreprise);
}

