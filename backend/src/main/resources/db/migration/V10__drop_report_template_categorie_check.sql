-- ============================================================
-- V10__drop_report_template_categorie_check.sql
-- Suppression de la contrainte CHECK rigide sur categorie pour supporter les personas B2C
-- ============================================================

ALTER TABLE report_template
    DROP CONSTRAINT IF EXISTS report_template_categorie_check;
