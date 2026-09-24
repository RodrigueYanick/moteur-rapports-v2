package com.rapports.moteur.dto.dtoTemplate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateVersionTreeDto {
    private UUID rootId;
    private UUID currentId;
    private int totalVersions;
    private TemplateVersionDto tree;

    @Builder.Default
    private List<TemplateVersionDto> flatHistory = new ArrayList<>();
}

