-- ============================================================
-- V5__add_code_entreprise_to_document.sql
-- Ségrégation stricte multi-tenant et Row-Level Security (RLS)
-- ============================================================

-- 1. Ajout de la colonne code_entreprise sur la table document
ALTER TABLE document
    ADD COLUMN IF NOT EXISTS code_entreprise VARCHAR(50);

-- 2. Backfill des documents existants depuis le template parent
UPDATE document d
SET code_entreprise = COALESCE(t.code_entreprise, 'ENT-001')
FROM report_template t
WHERE d.template_id = t.id AND (d.code_entreprise IS NULL OR d.code_entreprise = '');

-- Fallback pour tout document orphelin restant
UPDATE document
SET code_entreprise = 'ENT-001'
WHERE code_entreprise IS NULL OR code_entreprise = '';

-- 3. Application de la contrainte NOT NULL
ALTER TABLE document
    ALTER COLUMN code_entreprise SET NOT NULL;

-- 4. Index composite optimisé pour les requêtes par tenant triées par date
CREATE INDEX IF NOT EXISTS idx_document_entreprise_created ON document(code_entreprise, date_creation DESC);

-- 5. Activation du Row-Level Security (RLS) PostgreSQL
ALTER TABLE document ENABLE ROW LEVEL SECURITY;

-- Création de la politique de cloisonnement RLS (idempotente)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'document' AND policyname = 'tenant_isolation_policy'
    ) THEN
        CREATE POLICY tenant_isolation_policy ON document
            AS PERMISSIVE
            FOR ALL
            TO PUBLIC
            USING (
                current_setting('app.current_tenant', true) IS NULL 
                OR current_setting('app.current_tenant', true) = ''
                OR code_entreprise = current_setting('app.current_tenant', true)
            )
            WITH CHECK (
                current_setting('app.current_tenant', true) IS NULL 
                OR current_setting('app.current_tenant', true) = ''
                OR code_entreprise = current_setting('app.current_tenant', true)
            );
    END IF;
END
$$;

