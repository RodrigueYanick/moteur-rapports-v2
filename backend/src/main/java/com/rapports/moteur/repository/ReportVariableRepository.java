package com.rapports.moteur.repository;

import com.rapports.moteur.entity.ReportVariable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportVariableRepository extends JpaRepository<ReportVariable, UUID> {
    List<ReportVariable> findByTemplate_Id(UUID templateId);
}
