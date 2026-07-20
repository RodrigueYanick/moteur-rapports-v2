package com.rapports.moteur.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GenerateReportRequest(
        @NotNull UUID templateId,
        @NotBlank String format,
        @NotBlank String title
) {
}
