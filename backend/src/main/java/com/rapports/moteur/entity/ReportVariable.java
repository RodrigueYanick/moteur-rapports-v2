package com.rapports.moteur.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "report_variables")
public class ReportVariable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VariableType type;

    @Column(name = "description", length = 255)
    private String description;

    @Column(nullable = false)
    private boolean required;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ReportTemplate template;

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public VariableType getType() { return type; }
    public void setType(VariableType type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public ReportTemplate getTemplate() { return template; }
    public void setTemplate(ReportTemplate template) { this.template = template; }
}
