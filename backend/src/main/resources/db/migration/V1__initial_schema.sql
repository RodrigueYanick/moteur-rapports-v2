-- ============================================================
-- V1__initial_schema.sql
-- Schéma initial complet et idempotent pour le Moteur de Rapports
-- ============================================================

-- 1. Table des Entreprises (Tenants)
CREATE TABLE IF NOT EXISTS entreprise (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    nom VARCHAR(150) NOT NULL,
    email_contact VARCHAR(150),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation TIMESTAMP
);

-- 2. Table des Utilisateurs
CREATE TABLE IF NOT EXISTS app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(150) NOT NULL UNIQUE,
    mot_de_passe VARCHAR(255) NOT NULL,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100),
    role VARCHAR(30) NOT NULL,
    entreprise_id UUID REFERENCES entreprise(id) ON DELETE SET NULL,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation TIMESTAMP
);

-- 3. Table de Configuration de la Feuille de Travail par Entreprise
CREATE TABLE IF NOT EXISTS company_workspace_config (
    id UUID PRIMARY KEY,
    code_entreprise VARCHAR(50) NOT NULL UNIQUE,
    format_papier VARCHAR(10) NOT NULL DEFAULT 'A4',
    largeur_mm INTEGER,
    hauteur_mm INTEGER,
    mode_pagination VARCHAR(20) NOT NULL DEFAULT 'FIXED',
    marge_gauche_mm INTEGER NOT NULL DEFAULT 10,
    marge_droite_mm INTEGER NOT NULL DEFAULT 10,
    marge_haut_mm INTEGER NOT NULL DEFAULT 10,
    marge_bas_mm INTEGER NOT NULL DEFAULT 10,
    couleur_fond VARCHAR(30) DEFAULT '#ffffff',
    header_actif BOOLEAN NOT NULL DEFAULT FALSE,
    hauteur_header_mm INTEGER DEFAULT 15,
    header_contenu TEXT,
    header_alignement VARCHAR(20) DEFAULT 'LEFT',
    header_afficher_sur_premiere_page BOOLEAN NOT NULL DEFAULT TRUE,
    header_ligne_separation BOOLEAN NOT NULL DEFAULT FALSE,
    header_couleur_ligne VARCHAR(30) DEFAULT '#d1d5db',
    footer_actif BOOLEAN NOT NULL DEFAULT FALSE,
    hauteur_footer_mm INTEGER DEFAULT 15,
    footer_contenu TEXT,
    footer_alignement VARCHAR(20) DEFAULT 'LEFT',
    footer_afficher_sur_premiere_page BOOLEAN NOT NULL DEFAULT TRUE,
    footer_ligne_separation BOOLEAN NOT NULL DEFAULT FALSE,
    footer_couleur_ligne VARCHAR(30) DEFAULT '#d1d5db',
    numerotation_page BOOLEAN NOT NULL DEFAULT TRUE,
    format_numerotation VARCHAR(50) DEFAULT 'PAGE_X_SUR_Y',
    date_creation TIMESTAMP,
    date_modification TIMESTAMP
);

-- 4. Table des Modèles de Rapports (Templates)
CREATE TABLE IF NOT EXISTS report_template (
    id UUID PRIMARY KEY,
    nom VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    contenu_design JSONB,
    schema JSONB,
    code_entreprise VARCHAR(50),
    statut VARCHAR(50) NOT NULL,
    version INTEGER NOT NULL,
    date_creation TIMESTAMP,
    date_modification TIMESTAMP,
    categorie VARCHAR(50) NOT NULL,
    format_papier VARCHAR(10) NOT NULL DEFAULT 'A4',
    largeur_mm INTEGER,
    hauteur_mm INTEGER,
    mode_pagination VARCHAR(20) NOT NULL DEFAULT 'FIXED',
    marge_gauche_mm INTEGER DEFAULT 0,
    marge_droite_mm INTEGER DEFAULT 0,
    marge_haut_mm INTEGER DEFAULT 0,
    marge_bas_mm INTEGER DEFAULT 0,
    couleur_fond VARCHAR(30) DEFAULT '#ffffff',
    header_actif BOOLEAN NOT NULL DEFAULT FALSE,
    hauteur_header_mm INTEGER DEFAULT 15,
    header_contenu TEXT,
    header_alignement VARCHAR(20) DEFAULT 'LEFT',
    header_afficher_sur_premiere_page BOOLEAN NOT NULL DEFAULT TRUE,
    header_ligne_separation BOOLEAN NOT NULL DEFAULT FALSE,
    header_couleur_ligne VARCHAR(30) DEFAULT '#d1d5db',
    footer_actif BOOLEAN NOT NULL DEFAULT FALSE,
    hauteur_footer_mm INTEGER DEFAULT 15,
    footer_contenu TEXT,
    footer_alignement VARCHAR(20) DEFAULT 'LEFT',
    footer_afficher_sur_premiere_page BOOLEAN NOT NULL DEFAULT TRUE,
    footer_ligne_separation BOOLEAN NOT NULL DEFAULT FALSE,
    footer_couleur_ligne VARCHAR(30) DEFAULT '#d1d5db',
    numerotation_page BOOLEAN NOT NULL DEFAULT TRUE,
    format_numerotation VARCHAR(50) DEFAULT 'PAGE_X_SUR_Y',
    parent_template_id UUID REFERENCES report_template(id) ON DELETE SET NULL
);

-- 5. Table des Variables Explicites liées à un Template
CREATE TABLE IF NOT EXISTS report_variable (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES report_template(id) ON DELETE CASCADE,
    nom_variable VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    obligatoire BOOLEAN NOT NULL DEFAULT FALSE,
    description VARCHAR(500)
);

-- 6. Table de l'Historique des Générations de Documents
CREATE TABLE IF NOT EXISTS report_generation (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES report_template(id) ON DELETE CASCADE,
    donnees_recues JSONB,
    statut VARCHAR(50),
    url_fichier_genere VARCHAR(1000),
    date_generation TIMESTAMP
);

-- 7. Table des Documents Enregistrés
CREATE TABLE IF NOT EXISTS document (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES report_template(id) ON DELETE CASCADE,
    nom VARCHAR(255) NOT NULL,
    donnees JSONB,
    statut VARCHAR(50) NOT NULL,
    date_creation TIMESTAMP,
    date_modification TIMESTAMP
);
