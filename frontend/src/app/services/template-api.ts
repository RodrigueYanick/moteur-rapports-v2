import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Template, TemplateVersionTreeDto } from '../models/template.model';
import { TemplateSchema } from '../models/template-schema.model';
import { Generation } from '../models/generation.model';
import { TemplateForm } from '../models/template-form.model';
import { Variable } from '../models/variable.model';
import { GeneratedDocument } from '../models/Document.model';

@Injectable({
  providedIn: 'root',
})
export class TemplateApiService {
  private baseUrl = '/api/templates'; // le proxy redirige vers http://localhost:8081/api/templates

  constructor(private http: HttpClient) {}

  // Liste des templates avec visibilité et recherche
  getTemplates(visibilite?: string, q?: string): Observable<Template[]> {
    let params = new HttpParams();
    if (visibilite) params = params.set('visibilite', visibilite);
    if (q) params = params.set('q', q);
    return this.http.get<Template[]>(`${this.baseUrl}`, { params });
  }

  // Détail d'un template
  getTemplate(id: string): Observable<Template> {
    return this.http.get<Template>(`${this.baseUrl}/${id}`);
  }

  // Créer un nouveau template
  createTemplate(form: any): Observable<Template> {
    return this.http.post<Template>(`${this.baseUrl}`, form);
  }
  
  // Modifier un template
updateTemplate(id: string, form: any): Observable<Template> {
  return this.http.put<Template>(`${this.baseUrl}/${id}`, form);
}

  // Publier un template
  publishTemplate(id: string): Observable<Template> {
    return this.http.post<Template>(`${this.baseUrl}/${id}/publish`, {});
  }

  // Récupérer le schéma / dictionnaire de variables
  getSchema(id: string): Observable<TemplateSchema> {
    return this.http.get<TemplateSchema>(`${this.baseUrl}/${id}/schema`);
  }

  // Gestion des variables (pour l'instant manuelle, avant l'étape 3)
  getVariables(templateId: string): Observable<Variable[]> {
    return this.http.get<Variable[]>(`${this.baseUrl}/${templateId}/variables`);
  }

  addVariable(templateId: string, variable: Partial<Variable>): Observable<Variable> {
    return this.http.post<Variable>(`${this.baseUrl}/${templateId}/variables`, variable);
  }

  deleteVariable(templateId: string, variableId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${templateId}/variables/${variableId}`);
  }

  // Génération de document (renvoie un Blob PDF)
  generateDocument(id: string, data: any): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/${id}/generate`, data, { responseType: 'blob' });
  }

  updateVariable(
    templateId: string,
    variableId: string,
    variable: Partial<Variable>,
  ): Observable<Variable> {
    return this.http.put<Variable>(
      `${this.baseUrl}/${templateId}/variables/${variableId}`,
      variable,
    );
  }

  // Historique des générations
  getGenerations(templateId: string): Observable<Generation[]> {
    return this.http.get<Generation[]>(`${this.baseUrl}/${templateId}/generations`);
  }

  exportHtml(id: string, data: any): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/${id}/preview-html`, data, { responseType: 'blob' });
  }

  // Export Excel (.xlsx) direct avec les données
  exportExcel(id: string, data: any): Observable<Blob> {
    return this.http.post(`${this.baseUrl}/${id}/export-excel`, data, { responseType: 'blob' });
  }

  // Export Excel (.xlsx) d'un document sauvegardé
  exportDocumentExcel(templateId: string, documentId: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/${templateId}/documents/${documentId}/export-excel`, { responseType: 'blob' });
  }

  duplicateTemplate(id: string): Observable<Template> {
    return this.http.post<Template>(`${this.baseUrl}/${id}/duplicate`, {});
  }

  deleteTemplate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  createDocument(
    templateId: string,
    document: { nom: string; donnees: any },
  ): Observable<GeneratedDocument> {
    return this.http.post<GeneratedDocument>(`${this.baseUrl}/${templateId}/documents`, document);
  }

  updateDocument(
    templateId: string,
    documentId: string,
    document: { nom: string; donnees: any },
  ): Observable<GeneratedDocument> {
    return this.http.put<GeneratedDocument>(
      `${this.baseUrl}/${templateId}/documents/${documentId}`,
      document,
    );
  }

  getDocuments(templateId: string): Observable<GeneratedDocument[]> {
    return this.http.get<GeneratedDocument[]>(`${this.baseUrl}/${templateId}/documents`);
  }

  deleteDocument(templateId: string, documentId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${templateId}/documents/${documentId}`);
  }

  sendDocumentEmail(templateId: string, documentId: string, payload: { destinataire: string; objet: string; message?: string }): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/${templateId}/documents/${documentId}/send-email`, payload);
  }

  // Tous les documents avec visibilité et recherche
  getAllDocuments(visibilite?: string, q?: string): Observable<GeneratedDocument[]> {
    let params = new HttpParams();
    if (visibilite) params = params.set('visibilite', visibilite);
    if (q) params = params.set('q', q);
    return this.http.get<GeneratedDocument[]>('/api/documents', { params });
  }

  archiveTemplate(id: string): Observable<Template> {
    return this.http.post<Template>(`${this.baseUrl}/${id}/archive`, {});
  }

  newVersion(id: string): Observable<Template> {
    return this.http.post<Template>(`${this.baseUrl}/${id}/new-version`, {});
  }

  restoreTemplate(id: string): Observable<Template> {
    return this.http.post<Template>(`${this.baseUrl}/${id}/restore`, {});
  }

  getVersionTree(id: string): Observable<TemplateVersionTreeDto> {
    return this.http.get<TemplateVersionTreeDto>(`${this.baseUrl}/${id}/versions`);
  }

  getPreviewHtml(templateId: string, data: any): Observable<string> {
    return this.http.post(`${this.baseUrl}/${templateId}/preview-html`, data, { responseType: 'text' });
  }
  

  // Configuration de l'espace de travail de l'entreprise
  getWorkspaceConfig(): Observable<import('../models/workspace-config.model').WorkspaceConfig> {
    return this.http.get<import('../models/workspace-config.model').WorkspaceConfig>('/api/workspace-config');
  }

  updateWorkspaceConfig(config: Partial<import('../models/workspace-config.model').WorkspaceConfig>): Observable<import('../models/workspace-config.model').WorkspaceConfig> {
    return this.http.put<import('../models/workspace-config.model').WorkspaceConfig>('/api/workspace-config', config);
  }

  resetWorkspaceConfig(): Observable<import('../models/workspace-config.model').WorkspaceConfig> {
    return this.http.post<import('../models/workspace-config.model').WorkspaceConfig>('/api/workspace-config/reset', {});
  }

  // ==========================================
  // Traitement par lot (Batch) & Webhooks
  // ==========================================

  createBatch(templateId: string, request: import('../models/batch.model').BatchCreateRequest): Observable<import('../models/batch.model').BatchResponse> {
    return this.http.post<import('../models/batch.model').BatchResponse>(`${this.baseUrl}/${templateId}/batch`, request);
  }

  listBatches(templateId?: string): Observable<import('../models/batch.model').BatchResponse[]> {
    let params = new HttpParams();
    if (templateId) params = params.set('templateId', templateId);
    return this.http.get<import('../models/batch.model').BatchResponse[]>('/api/batches', { params });
  }

  getBatch(batchId: string): Observable<import('../models/batch.model').BatchResponse> {
    return this.http.get<import('../models/batch.model').BatchResponse>(`/api/batches/${batchId}`);
  }

  getBatchItems(batchId: string): Observable<import('../models/batch.model').BatchItemResponse[]> {
    return this.http.get<import('../models/batch.model').BatchItemResponse[]>(`/api/batches/${batchId}/items`);
  }

  retryFailedBatch(batchId: string): Observable<import('../models/batch.model').BatchResponse> {
    return this.http.post<import('../models/batch.model').BatchResponse>(`/api/batches/${batchId}/retry-failed`, {});
  }

  downloadBatchZip(batchId: string): Observable<Blob> {
    return this.http.get(`/api/batches/${batchId}/download-zip`, { responseType: 'blob' });
  }

  testWebhook(req: import('../models/batch.model').WebhookTestRequest): Observable<import('../models/batch.model').WebhookTestResponse> {
    return this.http.post<import('../models/batch.model').WebhookTestResponse>('/api/batches/webhooks/test', req);
  }
}

