package com.rapports.moteur.dto.dtoTemplate;

import com.rapports.moteur.entity.TemplateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateVersionDto {
    private UUID id;
    private String nom;
    private Integer version;
    private TemplateStatus statut;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private UUID parentTemplateId;
    private boolean isCurrent;

    @Builder.Default
    private List<TemplateVersionDto> children = new ArrayList<>();
}

