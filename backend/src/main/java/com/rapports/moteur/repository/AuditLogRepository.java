package com.rapports.moteur.repository;

import com.rapports.moteur.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {
    Page<AuditLog> findByCodeEntrepriseOrderByDateCreationDesc(String codeEntreprise, Pageable pageable);
    List<AuditLog> findByCodeEntrepriseOrderByDateCreationDesc(String codeEntreprise);

    @Query("SELECT a FROM AuditLog a WHERE a.codeEntreprise = :codeEntreprise " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:ressourceType IS NULL OR a.ressourceType = :ressourceType) " +
           "AND (:userEmail IS NULL OR LOWER(a.userEmail) LIKE LOWER(CONCAT('%', :userEmail, '%'))) " +
           "AND (:dateDebut IS NULL OR a.dateCreation >= :dateDebut) " +
           "AND (:dateFin IS NULL OR a.dateCreation <= :dateFin) " +
           "ORDER BY a.dateCreation DESC")
    Page<AuditLog> searchAuditLogs(
            @Param("codeEntreprise") String codeEntreprise,
            @Param("action") String action,
            @Param("ressourceType") String ressourceType,
            @Param("userEmail") String userEmail,
            @Param("dateDebut") LocalDateTime dateDebut,
            @Param("dateFin") LocalDateTime dateFin,
            Pageable pageable
    );

    long countByCodeEntreprise(String codeEntreprise);

    @Query("SELECT a.action, COUNT(a) FROM AuditLog a WHERE a.codeEntreprise = :codeEntreprise GROUP BY a.action")
    List<Object[]> countByActionGrouped(@Param("codeEntreprise") String codeEntreprise);
}