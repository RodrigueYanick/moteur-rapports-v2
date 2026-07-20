package com.rapports.moteur.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTemplateRequest(
        @NotBlank String name,
        @NotBlank String description
) {
}
