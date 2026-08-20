package com.rapports.moteur.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Data
@Entity
@Table(name = "report_variable")
@Getter 
@Setter
@NoArgsConstructor 
@AllArgsConstructor
@Builder
public class ReportVariable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ReportTemplate template;

    @Column(name = "nom_variable", nullable = false)
    private String nomVariable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VariableType type;

    @Column(nullable = false)
    private Boolean obligatoire;

    @Column(name = "description", length = 500)
    private String description;

}
