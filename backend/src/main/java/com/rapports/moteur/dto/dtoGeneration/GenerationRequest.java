package com.rapports.moteur.dto.dtoGeneration;

import lombok.Data;

@Data
public class GenerationRequest {
    // Les données envoyées par l'ERP (JSON libre)
    private Object data;
}