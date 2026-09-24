export type BatchStatus = 'EN_ATTENTE' | 'EN_COURS' | 'TERMINE' | 'TERMINE_AVEC_ERREURS' | 'ECHEC';

export type BatchItemStatus = 'EN_ATTENTE' | 'EN_COURS' | 'SUCCES' | 'ECHEC';

export type WebhookStatus = 'NON_CONFIGURE' | 'EN_ATTENTE' | 'ENVOYE' | 'ECHEC';

export interface BatchItemRequest {
  customId?: string;
  data: Record<string, any>;
}

export interface BatchCreateRequest {
  templateId?: string;
  items: BatchItemRequest[];
  webhookUrl?: string;
  webhookSecret?: string;
}

export interface BatchItemResponse {
  id: string;
  batchId: string;
  generationId?: string;
  customId?: string;
  statut: BatchItemStatus;
  urlFichier?: string;
  downloadUrl?: string;
  erreur?: string;
  dateTraitement?: string;
}

export interface BatchResponse {
  id: string;
  templateId: string;
  templateNom?: string;
  codeEntreprise?: string;
  statut: BatchStatus;
  totalItems: number;
  processedItems: number;
  successCount: number;
  failureCount: number;
  progressionPourcentage: number;
  webhookUrl?: string;
  webhookStatut: WebhookStatus;
  webhookTentatives: number;
  erreur?: string;
  dateCreation: string;
  dateFin?: string;
  dureeSecondes?: number;
  items?: BatchItemResponse[];
}

export interface WebhookTestRequest {
  url: string;
  secret?: string;
}

export interface WebhookTestResponse {
  succes: boolean;
  statusCode: number;
  message: string;
  tempsReponseMs: number;
}

