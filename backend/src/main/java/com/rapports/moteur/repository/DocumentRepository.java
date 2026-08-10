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

    @Query("SELECT d FROM Document d WHERE d.template.codeEntreprise = :code")
    List<Document> findByCodeEntreprise(@Param("code") String code);
}