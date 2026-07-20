package com.rapports.moteur.repository;

import com.rapports.moteur.entity.ReportTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReportTemplateRepository extends JpaRepository<ReportTemplate, UUID> {
}
