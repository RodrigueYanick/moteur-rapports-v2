package com.rapports.moteur.repository;

import com.rapports.moteur.entity.ReportGeneration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportGenerationRepository extends JpaRepository<ReportGeneration, UUID> {

    List<ReportGeneration> findByTemplateId(UUID id);

    List<ReportGeneration> findByTemplate_IdOrderByDateGenerationDesc(UUID templateId);

}
