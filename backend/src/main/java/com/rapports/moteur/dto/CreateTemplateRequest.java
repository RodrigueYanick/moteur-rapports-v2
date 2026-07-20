package com.rapports.moteur.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateTemplateRequest(
        @NotBlank String name,
        @NotBlank String description,
        @NotEmpty List<ReportVariableDTO> variables
) {
}
