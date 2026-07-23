package com.rapports.moteur.dto.dtoTemplate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

import com.rapports.moteur.entity.ReportVariable;


public record TemplateRequest(
        @NotBlank String name,
        @NotBlank String description,
        @NotBlank String contenuDesign,
        @NotEmpty List<ReportVariable> variables
) {
}
