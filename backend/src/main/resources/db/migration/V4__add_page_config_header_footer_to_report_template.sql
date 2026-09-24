-- ============================================================
-- V4__add_page_config_header_footer_to_report_template.sql
-- Ajout des colonnes de mise en page, entête et pied de page
-- sur la table report_template (rattrapage idempotent)
-- ============================================================

ALTER TABLE report_template
    ADD COLUMN IF NOT EXISTS format_papier VARCHAR(10) NOT NULL DEFAULT 'A4',
    ADD COLUMN IF NOT EXISTS largeur_mm INTEGER,
    ADD COLUMN IF NOT EXISTS hauteur_mm INTEGER,
    ADD COLUMN IF NOT EXISTS mode_pagination VARCHAR(20) NOT NULL DEFAULT 'FIXED',
    ADD COLUMN IF NOT EXISTS marge_gauche_mm INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS marge_droite_mm INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS marge_haut_mm INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS marge_bas_mm INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS couleur_fond VARCHAR(30) DEFAULT '#ffffff',
    ADD COLUMN IF NOT EXISTS header_actif BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS hauteur_header_mm INTEGER DEFAULT 15,
    ADD COLUMN IF NOT EXISTS header_contenu TEXT,
    ADD COLUMN IF NOT EXISTS header_alignement VARCHAR(20) DEFAULT 'LEFT',
    ADD COLUMN IF NOT EXISTS header_afficher_sur_premiere_page BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS header_ligne_separation BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS header_couleur_ligne VARCHAR(30) DEFAULT '#d1d5db',
    ADD COLUMN IF NOT EXISTS footer_actif BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS hauteur_footer_mm INTEGER DEFAULT 15,
    ADD COLUMN IF NOT EXISTS footer_contenu TEXT,
    ADD COLUMN IF NOT EXISTS footer_alignement VARCHAR(20) DEFAULT 'LEFT',
    ADD COLUMN IF NOT EXISTS footer_afficher_sur_premiere_page BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS footer_ligne_separation BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS footer_couleur_ligne VARCHAR(30) DEFAULT '#d1d5db',
    ADD COLUMN IF NOT EXISTS numerotation_page BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS format_numerotation VARCHAR(50) DEFAULT 'PAGE_X_SUR_Y';

