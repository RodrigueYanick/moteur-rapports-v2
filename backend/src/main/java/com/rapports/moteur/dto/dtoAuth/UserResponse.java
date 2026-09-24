package com.rapports.moteur.dto.dtoAuth;

import com.rapports.moteur.entity.Role;
import com.rapports.moteur.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String email;
    private String nom;
    private String prenom;
    private String nomComplet;
    private Role role;
    private String codeEntreprise;
    private String nomEntreprise;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .nomComplet(user.getNomComplet())
                .role(user.getRole())
                .codeEntreprise(user.getCodeEntreprise())
                .nomEntreprise(user.getEntreprise() != null ? user.getEntreprise().getNom() : null)
                .build();
    }
}

