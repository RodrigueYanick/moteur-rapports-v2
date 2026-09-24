package com.rapports.moteur.repository;

import com.rapports.moteur.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByTemplateIdAndCodeEntreprise(UUID templateId, String codeEntreprise);

    Optional<Document> findByIdAndCodeEntreprise(UUID id, String codeEntreprise);

    List<Document> findByCodeEntrepriseOrderByDateCreationDesc(String codeEntreprise);

    @Query("SELECT d FROM Document d WHERE d.codeEntreprise = :code AND LOWER(d.nom) LIKE LOWER(CONCAT('%', :q, '%')) ORDER BY d.dateCreation DESC")
    List<Document> findByCodeEntrepriseAndNomContainingIgnoreCaseOrderByDateCreationDesc(@Param("code") String code, @Param("q") String q);

    // Méthode de compatibilité
    List<Document> findByCodeEntreprise(String codeEntreprise);
}