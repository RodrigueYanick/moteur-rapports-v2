package com.rapports.moteur.dto.dtoProfile;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonaProfileDto {

    private Map<String, Object> personalInfo;
    private Object experiences;
    private Object formations;
    private Object competences;
    private Map<String, Object> preferences;
    private LocalDateTime dateModification;
}
