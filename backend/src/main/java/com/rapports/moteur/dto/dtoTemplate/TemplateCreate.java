package com.rapports.moteur.dto.dtoTemplate;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import com.rapports.moteur.entity.Categorie;

@Data
public class TemplateCreate {

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    private String description;

    private String contenuDesign;

    private Categorie categorie;
    
    private String formatPapier;
}
