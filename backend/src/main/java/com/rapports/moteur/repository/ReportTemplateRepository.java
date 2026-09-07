package com.rapports.moteur.repository;

import com.rapports.moteur.entity.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, UUID> {

    List<ReportTemplate> findByCodeEntreprise(String codeEntreprise);
    List<ReportTemplate> findByCodeEntrepriseOrCodeEntrepriseIsNull(String codeEntreprise);
    List<ReportTemplate> findByCodeEntrepriseIsNull();

    // Recherche avec filtre de visibilité et texte
    @Query("SELECT t FROM ReportTemplate t WHERE t.codeEntreprise = :code AND LOWER(t.nom) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<ReportTemplate> findByCodeEntrepriseAndNomContaining(@Param("code") String code, @Param("q") String q);

    @Query("SELECT t FROM ReportTemplate t WHERE (t.codeEntreprise = :code OR t.codeEntreprise IS NULL) AND LOWER(t.nom) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<ReportTemplate> findByCodeEntrepriseOrCodeEntrepriseIsNullAndNomContaining(@Param("code") String code, @Param("q") String q);

    @Query("SELECT t FROM ReportTemplate t WHERE t.codeEntreprise IS NULL AND LOWER(t.nom) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<ReportTemplate> findByCodeEntrepriseIsNullAndNomContaining(@Param("q") String q);
}