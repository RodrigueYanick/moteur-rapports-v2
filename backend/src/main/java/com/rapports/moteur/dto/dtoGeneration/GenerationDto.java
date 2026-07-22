package com.rapports.moteur.dto.dtoGeneration;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

import com.rapports.moteur.entity.GenerationStatus;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class GenerationDto {
    private UUID id;
    private UUID templateId;
    private LocalDateTime dateGeneration;
    private String donneesRecues;      
    private GenerationStatus statut;
    private String urlFichierGenere;
}