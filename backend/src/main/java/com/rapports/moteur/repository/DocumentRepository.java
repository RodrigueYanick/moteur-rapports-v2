package com.rapports.moteur.repository;

import com.rapports.moteur.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByTemplateId(UUID templateId);

    // Ancienne méthode (conservée pour compatibilité)
    @Query("SELECT d FROM Document d WHERE d.template.codeEntreprise = :code")
    List<Document> findByCodeEntreprise(@Param("code") String code);

    // --- Nouvelles méthodes pour visibilité + recherche ---

    @Query("SELECT d FROM Document d WHERE d.template.codeEntreprise IS NULL")
    List<Document> findByTemplateCodeEntrepriseIsNull();

    @Query("SELECT d FROM Document d WHERE d.template.codeEntreprise = :code")
    List<Document> findByTemplateCodeEntreprise(@Param("code") String code);

    @Query("SELECT d FROM Document d WHERE d.template.codeEntreprise = :code OR d.template.codeEntreprise IS NULL")
    List<Document> findByTemplateCodeEntrepriseOrTemplateCodeEntrepriseIsNull(@Param("code") String code);

    @Query("SELECT d FROM Document d WHERE d.template.codeEntreprise = :code AND LOWER(d.nom) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Document> findByTemplateCodeEntrepriseAndNomContaining(@Param("code") String code, @Param("q") String q);

    @Query("SELECT d FROM Document d WHERE d.template.codeEntreprise IS NULL AND LOWER(d.nom) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Document> findByTemplateCodeEntrepriseIsNullAndNomContaining(@Param("q") String q);

    @Query("SELECT d FROM Document d WHERE (d.template.codeEntreprise = :code OR d.template.codeEntreprise IS NULL) AND LOWER(d.nom) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Document> findByTemplateCodeEntrepriseOrTemplateCodeEntrepriseIsNullAndNomContaining(@Param("code") String code, @Param("q") String q);
}