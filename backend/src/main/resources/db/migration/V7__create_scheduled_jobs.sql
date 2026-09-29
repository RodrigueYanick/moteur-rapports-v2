-- ============================================================
-- V7__create_scheduled_jobs.sql
-- Planificateur de rapports récurrents (CRON Scheduler),
-- historique d'exécution et verrous distribués ShedLock
-- ============================================================

-- 1. Table des Tâches Planifiées (scheduled_report_job)
CREATE TABLE IF NOT EXISTS scheduled_report_job (
    id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES report_template(id) ON DELETE CASCADE,
    code_entreprise VARCHAR(50) NOT NULL,
    nom VARCHAR(255) NOT NULL,
    cron_expression VARCHAR(100) NOT NULL,
    fuseau_horaire VARCHAR(50) NOT NULL DEFAULT 'UTC',
    format_export VARCHAR(20) NOT NULL DEFAULT 'PDF',
    parametres JSONB,
    destinataires_emails TEXT,
    webhook_url VARCHAR(1000),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    prochaine_execution TIMESTAMP,
    derniere_execution TIMESTAMP,
    dernier_statut VARCHAR(30),
    date_creation TIMESTAMP NOT NULL,
    date_modification TIMESTAMP
);

-- 2. Table de l'Historique des Exécutions (scheduled_job_execution)
CREATE TABLE IF NOT EXISTS scheduled_job_execution (
    id UUID PRIMARY KEY,
    job_id UUID NOT NULL REFERENCES scheduled_report_job(id) ON DELETE CASCADE,
    date_debut TIMESTAMP NOT NULL,
    date_fin TIMESTAMP,
    statut VARCHAR(30) NOT NULL,
    destinataires_notifies INTEGER DEFAULT 0,
    duree_ms BIGINT,
    message_erreur TEXT
);

-- 3. Table des Verrous Distribués ShedLock
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- 4. Index de performance
CREATE INDEX IF NOT EXISTS idx_scheduled_job_template_id ON scheduled_report_job(template_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_job_code_entreprise ON scheduled_report_job(code_entreprise);
CREATE INDEX IF NOT EXISTS idx_scheduled_job_prochaine_exec ON scheduled_report_job(prochaine_execution);
CREATE INDEX IF NOT EXISTS idx_scheduled_job_actif ON scheduled_report_job(actif);
CREATE INDEX IF NOT EXISTS idx_scheduled_exec_job_id ON scheduled_job_execution(job_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_exec_date_debut ON scheduled_job_execution(date_debut DESC);
