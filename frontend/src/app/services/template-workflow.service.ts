import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Template } from '../models/template.model';
import { TemplateWorkflowHistory, WorkflowActionRequest } from '../models/workflow.model';

@Injectable({
  providedIn: 'root'
})
export class TemplateWorkflowService {
  private readonly baseUrl = '/api/templates';

  constructor(private http: HttpClient) {}

  submitForReview(templateId: string, request?: WorkflowActionRequest): Observable<Template> {
    return this.http.post<Template>(`${this.baseUrl}/${templateId}/workflow/submit`, request || {});
  }

  approve(templateId: string, request?: WorkflowActionRequest): Observable<Template> {
    return this.http.post<Template>(`${this.baseUrl}/${templateId}/workflow/approve`, request || {});
  }

  reject(templateId: string, request: WorkflowActionRequest): Observable<Template> {
    return this.http.post<Template>(`${this.baseUrl}/${templateId}/workflow/reject`, request);
  }

  publish(templateId: string, request?: WorkflowActionRequest): Observable<Template> {
    return this.http.post<Template>(`${this.baseUrl}/${templateId}/workflow/publish`, request || {});
  }

  getHistory(templateId: string): Observable<TemplateWorkflowHistory[]> {
    return this.http.get<TemplateWorkflowHistory[]>(`${this.baseUrl}/${templateId}/workflow/history`);
  }
}
