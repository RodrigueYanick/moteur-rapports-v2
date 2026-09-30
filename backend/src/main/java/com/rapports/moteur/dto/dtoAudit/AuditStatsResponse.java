package com.rapports.moteur.dto.dtoAudit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditStatsResponse {
    private long totalLogs;
    private Map<String, Long> countByAction;
}