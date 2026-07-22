package com.rapports.moteur.dto.dtoTemplate;

import com.rapports.moteur.entity.TemplateStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class TemplateResponse {

    private UUID id;
    private String nom;
    private String description;
    private String contenuDesign;
    private TemplateStatus statut;
    private Integer version;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    
}
