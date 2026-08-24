package com.rapports.moteur.repository;

import com.rapports.moteur.entity.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, UUID> {
    List<ReportTemplate> findByCodeEntreprise(String codeEntreprise);
    List<ReportTemplate> findByCodeEntrepriseOrCodeEntrepriseIsNull(String codeEntreprise);
    List<ReportTemplate> findByCodeEntrepriseIsNull();
}
