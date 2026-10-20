-- ============================================================
-- V9__add_public_personal_persona_and_sharing.sql
-- Evolution B2C / B2B : Compte Personnel, Persona, Profil Universel & Partage Public
-- ============================================================

-- 1. Champs Compte Personnel & Persona sur app_user
ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS account_type VARCHAR(30) DEFAULT 'PERSONAL';

ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS target_persona VARCHAR(30);

UPDATE app_user
SET account_type = 'ENTERPRISE'
WHERE entreprise_id IS NOT NULL AND account_type IS NULL;

UPDATE app_user
SET account_type = 'PERSONAL'
WHERE account_type IS NULL;

-- 2. Champs de Partage Viral & Public sur document
ALTER TABLE document
    ADD COLUMN IF NOT EXISTS share_token VARCHAR(64);

ALTER TABLE document
    ADD COLUMN IF NOT EXISTS is_public_shared BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_document_share_token ON document(share_token);

-- 3. Table du Profil Universel (Mes Données B2C réutilisables)
CREATE TABLE IF NOT EXISTS persona_profile (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    personal_info JSONB,
    experiences JSONB,
    formations JSONB,
    competences JSONB,
    preferences JSONB,
    date_modification TIMESTAMP,
    CONSTRAINT uq_persona_profile_user UNIQUE (user_id)
);

CREATE INDEX IF NOT EXISTS idx_persona_profile_user_id ON persona_profile(user_id);

-- 4. Modèles de rapports publics sans code_entreprise obligatoire
ALTER TABLE report_template
    ALTER COLUMN code_entreprise DROP NOT NULL;
