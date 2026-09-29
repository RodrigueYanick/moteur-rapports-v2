package com.rapports.moteur.repository;

import com.rapports.moteur.entity.ScheduledReportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduledReportJobRepository extends JpaRepository<ScheduledReportJob, UUID> {

    List<ScheduledReportJob> findByActifTrueAndProchaineExecutionBefore(LocalDateTime threshold);

    List<ScheduledReportJob> findByCodeEntrepriseOrderByDateCreationDesc(String codeEntreprise);

    Optional<ScheduledReportJob> findByIdAndCodeEntreprise(UUID id, String codeEntreprise);

    List<ScheduledReportJob> findByTemplate_IdAndCodeEntrepriseOrderByDateCreationDesc(UUID templateId, String codeEntreprise);
}
