package com.rapports.moteur.dto.dtoDocument;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentCreate {
    @NotBlank
    private String nom;

    @NotNull
    private Object donnees;   // sera sérialisé en JSON par le service
}