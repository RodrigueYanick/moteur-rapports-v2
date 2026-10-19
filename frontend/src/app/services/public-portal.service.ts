import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TemplateResponse } from '../models/template.model';

export interface PublicCategory {
  code: string;
  label: string;
  persona: 'STUDENT' | 'CANDIDATE' | 'FREELANCE' | 'INDIVIDUAL';
  description: string;
  icon: string;
}

export interface PublicStats {
  totalDocuments: number;
  totalUsers: number;
  totalTemplates: number;
  satisfactionRate: number;
  activeCountries: number;
  availableCategories: number;
  lastUpdated?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PublicPortalService {
  private http = inject(HttpClient);
  private apiUrl = '/api/public';

  getPublicStats(): Observable<PublicStats> {
    return this.http.get<PublicStats>(`${this.apiUrl}/stats`);
  }

  getPublicTemplates(categorie?: string, q?: string): Observable<TemplateResponse[]> {
    let params = new HttpParams();
    if (categorie) {
      params = params.set('categorie', categorie);
    }
    if (q) {
      params = params.set('q', q);
    }
    return this.http.get<TemplateResponse[]>(`${this.apiUrl}/templates`, { params });
  }

  getPublicTemplate(id: string): Observable<TemplateResponse> {
    return this.http.get<TemplateResponse>(`${this.apiUrl}/templates/${id}`);
  }

  getPublicCategories(): Observable<PublicCategory[]> {
    return this.http.get<PublicCategory[]>(`${this.apiUrl}/categories`);
  }

  demoRender(id: string, data?: Record<string, unknown>): Observable<Blob> {
    return this.http.post(`${this.apiUrl}/templates/${id}/demo-render`, data || {}, {
      responseType: 'blob'
    });
  }
}

