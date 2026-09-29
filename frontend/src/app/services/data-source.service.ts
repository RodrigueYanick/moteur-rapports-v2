import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DataSourceConfig, DataSourceTestResult } from '../models/data-source.model';

@Injectable({
  providedIn: 'root'
})
export class DataSourceService {
  private baseUrl = '/api/data-sources';

  constructor(private http: HttpClient) {}

  getAll(): Observable<DataSourceConfig[]> {
    return this.http.get<DataSourceConfig[]>(this.baseUrl);
  }

  getById(id: string): Observable<DataSourceConfig> {
    return this.http.get<DataSourceConfig>(`${this.baseUrl}/${id}`);
  }

  create(source: Partial<DataSourceConfig>): Observable<DataSourceConfig> {
    return this.http.post<DataSourceConfig>(this.baseUrl, source);
  }

  update(id: string, source: Partial<DataSourceConfig>): Observable<DataSourceConfig> {
    return this.http.put<DataSourceConfig>(`${this.baseUrl}/${id}`, source);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  testConnection(source: Partial<DataSourceConfig>): Observable<DataSourceTestResult> {
    return this.http.post<DataSourceTestResult>(`${this.baseUrl}/test`, source);
  }

  executeQuery(id: string, query: string, params: Record<string, any> = {}, maxRows = 100): Observable<any> {
    return this.http.post<any>(`${this.baseUrl}/${id}/execute`, {
      requeteOuEndpoint: query,
      parametres: params,
      maxLignes: maxRows
    });
  }
}
