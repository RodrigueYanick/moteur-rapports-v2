export type ReportExportFormat = 'PDF' | 'EXCEL' | 'PNG_HD' | 'JPEG' | 'CSV' | 'JSON';
export type JobExecutionStatus = 'SUCCES' | 'ECHEC' | 'EN_COURS' | 'ANNULE';

export interface ScheduledJobRequest {
  nom: string;
  description?: string;
  templateId: string;
  cronExpression: string;
  timezone?: string;
  format: ReportExportFormat;
  parametresJson?: string;
  emailsDestinataires?: string[];
  webhookUrl?: string;
  actif?: boolean;
}

export interface ScheduledJobResponse {
  id: string;
  codeEntreprise: string;
  nom: string;
  description?: string;
  templateId: string;
  templateNom: string;
  cronExpression: string;
  timezone: string;
  format: ReportExportFormat;
  parametresJson?: string;
  emailsDestinataires?: string[];
  webhookUrl?: string;
  actif: boolean;
  derniereExecution?: string;
  statutDerniereExecution?: JobExecutionStatus;
  prochaineExecution?: string;
  dateCreation: string;
  dateModification: string;
}

export interface ScheduledJobExecutionResponse {
  id: string;
  jobId: string;
  jobNom: string;
  dateDebut: string;
  dateFin?: string;
  dureeMs?: number;
  statut: JobExecutionStatus;
  messageErreur?: string;
  urlFichierGenere?: string;
  notificationsEnvoyees?: string;
}
