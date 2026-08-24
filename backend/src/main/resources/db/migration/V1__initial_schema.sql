-- ============================================================
-- V1__initial_schema.sql
-- Schéma initial de la base de données du Moteur de Rapports
-- ============================================================

-- Table des modèles de rapports (templates)
CREATE TABLE report_template (
    id UUID PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    contenu_design JSONB,
    schema JSONB,
    code_entreprise VARCHAR(50) NOT NULL,
    statut VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL,
    date_creation TIMESTAMP,
    date_modification TIMESTAMP,
    categorie VARCHAR(50) NOT NULL,
    format_papier VARCHAR(10) NOT NULL DEFAULT 'A4',
    parent_template_id UUID,
    CONSTRAINT fk_parent_template
        FOREIGN KEY (parent_template_id)
        REFERENCES report_template(id)
        ON DELETE SET NULL
);

-- Table des variables explicites liées à un template
CREATE TABLE report_variable (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL,
    nom_variable VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    obligatoire BOOLEAN NOT NULL DEFAULT FALSE,
    description VARCHAR(1000),
    CONSTRAINT fk_variable_template
        FOREIGN KEY (template_id)
        REFERENCES report_template(id)
        ON DELETE CASCADE
);

-- Table de l'historique des générations de documents
CREATE TABLE report_generation (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL,
    donnees_recues JSONB,
    statut VARCHAR(50) NOT NULL,
    url_fichier_genere VARCHAR(1000),
    date_generation TIMESTAMP,
    CONSTRAINT fk_generation_template
        FOREIGN KEY (template_id)
        REFERENCES report_template(id)
        ON DELETE CASCADE
);

-- Table des documents générés (enregistrés)
CREATE TABLE document (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL,
    nom VARCHAR(255) NOT NULL,
    donnees JSONB,
    statut VARCHAR(50) NOT NULL,
    date_creation TIMESTAMP,
    date_modification TIMESTAMP,
    CONSTRAINT fk_document_template
        FOREIGN KEY (template_id)
        REFERENCES report_template(id)
        ON DELETE CASCADE
);

-- Index pour améliorer les performances des recherches par entreprise
CREATE INDEX idx_report_template_code_entreprise ON report_template(code_entreprise);
CREATE INDEX idx_report_variable_template_id ON report_variable(template_id);
CREATE INDEX idx_report_generation_template_id ON report_generation(template_id);
CREATE INDEX idx_document_template_id ON document(template_id);
