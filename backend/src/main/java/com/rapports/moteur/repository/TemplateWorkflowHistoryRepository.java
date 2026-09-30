package com.rapports.moteur.repository;

import com.rapports.moteur.entity.TemplateWorkflowHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface TemplateWorkflowHistoryRepository extends JpaRepository<TemplateWorkflowHistory, UUID> {
    List<TemplateWorkflowHistory> findByTemplateIdOrderByDateActionDesc(UUID templateId);
    List<TemplateWorkflowHistory> findByCodeEntrepriseOrderByDateActionDesc(String codeEntreprise);
}