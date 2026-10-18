package com.rapports.moteur.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "persona_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonaProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "personal_info", columnDefinition = "jsonb")
    private String personalInfo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "experiences", columnDefinition = "jsonb")
    private String experiences;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "formations", columnDefinition = "jsonb")
    private String formations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "competences", columnDefinition = "jsonb")
    private String competences;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferences", columnDefinition = "jsonb")
    private String preferences;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.dateModification = LocalDateTime.now();
    }
}
