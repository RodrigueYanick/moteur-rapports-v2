package com.rapports.moteur.dto.dtoGeneration;

import java.util.UUID;

import com.rapports.moteur.entity.ReportTemplate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GenerateRequest(
        @NotBlank String format,
        @NotBlank String title,
        @NotNull ReportTemplate template
) {
}
