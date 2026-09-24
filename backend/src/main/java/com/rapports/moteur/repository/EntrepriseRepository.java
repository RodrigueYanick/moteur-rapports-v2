package com.rapports.moteur.repository;

import com.rapports.moteur.entity.Entreprise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EntrepriseRepository extends JpaRepository<Entreprise, UUID> {
    Optional<Entreprise> findByCode(String code);
    boolean existsByCode(String code);
}

