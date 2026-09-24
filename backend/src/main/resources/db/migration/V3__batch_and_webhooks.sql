-- ============================================================
-- V3__batch_and_webhooks.sql
-- Gestion des générations par lot (Batch) et des Webhooks
-- ============================================================

-- 1. Table des Lots de Génération (report_batch)
CREATE TABLE IF NOT EXISTS report_batch (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES report_template(id) ON DELETE CASCADE,
    code_entreprise VARCHAR(50),
    statut VARCHAR(30) NOT NULL,
    total_items INTEGER NOT NULL DEFAULT 0,
    processed_items INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    failure_count INTEGER NOT NULL DEFAULT 0,
    webhook_url VARCHAR(1000),
    webhook_secret VARCHAR(255),
    webhook_statut VARCHAR(30) NOT NULL DEFAULT 'NON_CONFIGURE',
    webhook_tentatives INTEGER NOT NULL DEFAULT 0,
    erreur TEXT,
    date_creation TIMESTAMP NOT NULL,
    date_fin TIMESTAMP
);

-- 2. Table des Éléments d'un Lot (batch_generation_item)
CREATE TABLE IF NOT EXISTS batch_generation_item (
    id UUID PRIMARY KEY,
    batch_id UUID NOT NULL REFERENCES report_batch(id) ON DELETE CASCADE,
    generation_id UUID REFERENCES report_generation(id) ON DELETE SET NULL,
    custom_id VARCHAR(100),
    statut VARCHAR(30) NOT NULL,
    url_fichier VARCHAR(500),
    erreur TEXT,
    donnees JSONB,
    date_traitement TIMESTAMP
);

-- 3. Index de Performance
CREATE INDEX IF NOT EXISTS idx_report_batch_template_id ON report_batch(template_id);
CREATE INDEX IF NOT EXISTS idx_report_batch_code_entreprise ON report_batch(code_entreprise);
CREATE INDEX IF NOT EXISTS idx_report_batch_statut ON report_batch(statut);
CREATE INDEX IF NOT EXISTS idx_report_batch_date_creation ON report_batch(date_creation DESC);
CREATE INDEX IF NOT EXISTS idx_batch_item_batch_id ON batch_generation_item(batch_id);
CREATE INDEX IF NOT EXISTS idx_batch_item_statut ON batch_generation_item(statut);

