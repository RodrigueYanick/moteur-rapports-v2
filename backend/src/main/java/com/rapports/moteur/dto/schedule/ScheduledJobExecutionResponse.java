package com.rapports.moteur.dto.schedule;

import com.rapports.moteur.entity.JobExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledJobExecutionResponse {
    private UUID id;
    private UUID jobId;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private JobExecutionStatus statut;
    private Integer destinatairesNotifies;
    private Long dureeMs;
    private String messageErreur;
}
