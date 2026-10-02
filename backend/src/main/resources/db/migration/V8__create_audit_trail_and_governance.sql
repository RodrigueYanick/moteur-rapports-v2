-- Migration V8 : Audit Trail, Gouvernance et Certificats d'Entreprise

-- 1. Table audit_log
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    code_entreprise VARCHAR(50),
    user_id UUID,
    user_email VARCHAR(100),
    user_role VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    ressource_type VARCHAR(50) NOT NULL,
    ressource_id VARCHAR(100),
    adresse_ip VARCHAR(50),
    user_agent VARCHAR(255),
    details_json TEXT,
    statut VARCHAR(20) NOT NULL DEFAULT 'SUCCES'
);

CREATE INDEX IF NOT EXISTS idx_audit_code_entreprise ON audit_log(code_entreprise);
CREATE INDEX IF NOT EXISTS idx_audit_date_creation ON audit_log(date_creation);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_ressource ON audit_log(ressource_type, ressource_id);

-- 2. Table company_certificate
CREATE TABLE IF NOT EXISTS company_certificate (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code_entreprise VARCHAR(50) NOT NULL,
    nom VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    type VARCHAR(30) NOT NULL DEFAULT 'PKCS12',
    fichier_certificat_base64 TEXT NOT NULL,
    mot_de_passe_chiffre VARCHAR(255) NOT NULL,
    alias_certificat VARCHAR(100),
    emetteur VARCHAR(255),
    sujet VARCHAR(255),
    date_expiration TIMESTAMP,
    actif BOOLEAN NOT NULL DEFAULT true,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cert_code_entreprise ON company_certificate(code_entreprise);
CREATE INDEX IF NOT EXISTS idx_cert_actif ON company_certificate(actif);

-- 3. Table template_workflow_history
CREATE TABLE IF NOT EXISTS template_workflow_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL,
    code_entreprise VARCHAR(50) NOT NULL,
    user_id UUID,
    user_email VARCHAR(100),
    ancien_statut VARCHAR(30) NOT NULL,
    nouveau_statut VARCHAR(30) NOT NULL,
    commentaire TEXT,
    date_action TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workflow_template_id ON template_workflow_history(template_id);
CREATE INDEX IF NOT EXISTS idx_workflow_code_entreprise ON template_workflow_history(code_entreprise);
CREATE INDEX IF NOT EXISTS idx_workflow_date_action ON template_workflow_history(date_action);
