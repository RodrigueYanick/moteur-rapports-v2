-- Add margin columns to report_templates
ALTER TABLE report_templates ADD COLUMN marge_gauche_mm INTEGER DEFAULT 0;
ALTER TABLE report_templates ADD COLUMN marge_droite_mm INTEGER DEFAULT 0;
ALTER TABLE report_templates ADD COLUMN marge_haut_mm INTEGER DEFAULT 0;
ALTER TABLE report_templates ADD COLUMN marge_bas_mm INTEGER DEFAULT 0;
