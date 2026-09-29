package com.rapports.moteur.repository;

import com.rapports.moteur.entity.ScheduledJobExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledJobExecutionRepository extends JpaRepository<ScheduledJobExecution, UUID> {

    List<ScheduledJobExecution> findByJob_IdOrderByDateDebutDesc(UUID jobId);

    void deleteByJob_Id(UUID jobId);
}
