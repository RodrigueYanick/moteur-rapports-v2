import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Template } from '../models/template.model';
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

  // Liste tous les templates
  getTemplates(): Observable<Template[]> {
    return this.http.get<Template[]>(`${this.baseUrl}`);
  }

  // Détail d'un template
  getTemplate(id: string): Observable<Template> {
    return this.http.get<Template>(`${this.baseUrl}/${id}`);
  }

  // Créer un nouveau template
  createTemplate(form: TemplateForm): Observable<Template> {
    return this.http.post<Template>(`${this.baseUrl}`, form);
  }

  // Modifier un template
  updateTemplate(id: string, form: TemplateForm): Observable<Template> {
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

  getAllDocuments(): Observable<GeneratedDocument[]> {
    return this.http.get<GeneratedDocument[]>('/api/documents');
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
}
