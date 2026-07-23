package com.rapports.moteur.mapper;

import com.rapports.moteur.dto.dtoGeneration.GenerationDto;
import com.rapports.moteur.entity.ReportGeneration;
import org.springframework.stereotype.Component;

@Component
public class GenerationMapper {

    public GenerationDto toDto(ReportGeneration entity) {
        return GenerationDto.builder()
                .id(entity.getId())
                .templateId(entity.getTemplate().getId())
                .dateGeneration(entity.getDateGeneration())
                .donneesRecues(entity.getDonneesRecues())
                .statut(entity.getStatut())
                .urlFichierGenere(entity.getUrlFichierGenere())
                .build();
    }
    
}
