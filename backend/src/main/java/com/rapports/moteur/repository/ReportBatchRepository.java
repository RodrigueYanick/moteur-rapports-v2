package com.rapports.moteur.repository;

import com.rapports.moteur.entity.BatchStatus;
import com.rapports.moteur.entity.ReportBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportBatchRepository extends JpaRepository<ReportBatch, UUID> {

    List<ReportBatch> findByTemplate_IdOrderByDateCreationDesc(UUID templateId);

    @Query("SELECT b FROM ReportBatch b WHERE b.codeEntreprise = :codeEntreprise ORDER BY b.dateCreation DESC")
    List<ReportBatch> findAllByEntreprise(@Param("codeEntreprise") String codeEntreprise);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"template"})
    @Query("SELECT b FROM ReportBatch b WHERE b.id = :id")
    java.util.Optional<ReportBatch> findByIdWithTemplate(@Param("id") UUID id);

    List<ReportBatch> findByStatut(BatchStatus statut);
}

