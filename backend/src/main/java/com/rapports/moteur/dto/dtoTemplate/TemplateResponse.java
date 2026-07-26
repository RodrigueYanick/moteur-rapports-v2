package com.rapports.moteur.dto.dtoTemplate;

import com.rapports.moteur.entity.Categorie;
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
    private Categorie categorie;
    private String formatPapier;
    private TemplateStatus statut;
    private Integer version;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    
}
