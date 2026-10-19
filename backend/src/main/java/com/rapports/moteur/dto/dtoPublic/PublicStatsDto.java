package com.rapports.moteur.dto.dtoPublic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicStatsDto {

    private long totalDocuments;
    private long totalUsers;
    private long totalTemplates;
    private double satisfactionRate;
    private int activeCountries;
    private int availableCategories;
    private LocalDateTime lastUpdated;
}
