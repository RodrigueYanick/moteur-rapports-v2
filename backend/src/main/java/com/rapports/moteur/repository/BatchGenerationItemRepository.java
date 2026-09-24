package com.rapports.moteur.repository;

import com.rapports.moteur.entity.BatchGenerationItem;
import com.rapports.moteur.entity.BatchItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BatchGenerationItemRepository extends JpaRepository<BatchGenerationItem, UUID> {

    List<BatchGenerationItem> findByBatch_IdOrderByDateTraitementAsc(UUID batchId);

    List<BatchGenerationItem> findByBatch_IdAndStatut(UUID batchId, BatchItemStatus statut);

    long countByBatch_IdAndStatut(UUID batchId, BatchItemStatus statut);
}

