package com.rapports.moteur.repository;

import com.rapports.moteur.entity.CompanyCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyCertificateRepository extends JpaRepository<CompanyCertificate, UUID> {
    List<CompanyCertificate> findByCodeEntrepriseOrderByDateCreationDesc(String codeEntreprise);
    List<CompanyCertificate> findByCodeEntrepriseAndActifTrueOrderByDateCreationDesc(String codeEntreprise);
    Optional<CompanyCertificate> findByIdAndCodeEntreprise(UUID id, String codeEntreprise);
    boolean existsByCodeEntrepriseAndNom(String codeEntreprise, String nom);
}