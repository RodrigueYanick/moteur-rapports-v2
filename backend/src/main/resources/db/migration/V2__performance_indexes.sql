-- ============================================================
-- V2__performance_indexes.sql
-- Index de performance et optimisation des requêtes
-- ============================================================

-- 1. Index Entreprise et Utilisateurs
CREATE INDEX IF NOT EXISTS idx_entreprise_code ON entreprise(code);
CREATE INDEX IF NOT EXISTS idx_user_email ON app_user(email);
CREATE INDEX IF NOT EXISTS idx_user_entreprise_id ON app_user(entreprise_id);

-- 2. Index Configuration Espace de Travail
CREATE INDEX IF NOT EXISTS idx_company_workspace_config_code ON company_workspace_config(code_entreprise);

-- 3. Index Modèles de Rapports (Templates)
CREATE INDEX IF NOT EXISTS idx_report_template_code_entreprise ON report_template(code_entreprise);
CREATE INDEX IF NOT EXISTS idx_report_template_statut ON report_template(statut);
CREATE INDEX IF NOT EXISTS idx_report_template_parent_id ON report_template(parent_template_id);
CREATE INDEX IF NOT EXISTS idx_report_template_categorie ON report_template(categorie);

-- 4. Index Variables
CREATE INDEX IF NOT EXISTS idx_report_variable_template_id ON report_variable(template_id);

-- 5. Index Générations de Rapports
CREATE INDEX IF NOT EXISTS idx_report_generation_template_id ON report_generation(template_id);
CREATE INDEX IF NOT EXISTS idx_report_generation_statut ON report_generation(statut);
CREATE INDEX IF NOT EXISTS idx_report_generation_date ON report_generation(date_generation DESC);

-- 6. Index Documents
CREATE INDEX IF NOT EXISTS idx_document_template_id ON document(template_id);
CREATE INDEX IF NOT EXISTS idx_document_statut ON document(statut);

