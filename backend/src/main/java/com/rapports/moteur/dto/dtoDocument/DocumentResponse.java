package com.rapports.moteur.dto.dtoDocument;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentResponse {
    private UUID id;
    private UUID templateId;
    private String nom;
    private String templateNom;
    private Object donnees;
    private String statut;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
}