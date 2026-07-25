package com.rapports.moteur.dto.dtoTemplate;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TemplateCreate {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    private String description;

    private String contenuDesign;
}
