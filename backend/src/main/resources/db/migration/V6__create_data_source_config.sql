-- V6__create_data_source_config.sql
-- Moteur de sources de données distantes (SQL & REST)

CREATE TABLE IF NOT EXISTS data_source_config (
    id UUID PRIMARY KEY,
    code_entreprise VARCHAR(50) NOT NULL REFERENCES entreprise(code) ON DELETE CASCADE,
    nom VARCHAR(150) NOT NULL,
    type VARCHAR(30) NOT NULL, -- 'POSTGRESQL', 'MYSQL', 'REST_API'
    url_ou_hote VARCHAR(500) NOT NULL,
    port INTEGER,
    nom_base VARCHAR(100),
    nom_utilisateur VARCHAR(150),
    mot_de_passe_chiffre TEXT,
    en_tetes_json TEXT,
    methode_http VARCHAR(10) DEFAULT 'GET',
    auth_type VARCHAR(30) DEFAULT 'NONE', -- 'NONE', 'BASIC', 'BEARER', 'API_KEY'
    api_key_header VARCHAR(100),
    timeout_secondes INTEGER DEFAULT 10,
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    date_creation TIMESTAMP NOT NULL DEFAULT NOW(),
    date_modification TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_data_source_entreprise ON data_source_config(code_entreprise);

-- Liaison avec les modèles de rapports (report_template)
ALTER TABLE report_template 
ADD COLUMN IF NOT EXISTS data_source_id UUID REFERENCES data_source_config(id) ON DELETE SET NULL,
ADD COLUMN IF NOT EXISTS data_source_query TEXT,
ADD COLUMN IF NOT EXISTS data_source_mapping JSONB;
