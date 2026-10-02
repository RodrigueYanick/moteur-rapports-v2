import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuditLog, AuditSearchFilter, AuditStats } from '../models/audit.model';

@Injectable({
  providedIn: 'root'
})
export class AuditService {
  private readonly baseUrl = '/api/audit-logs';

  constructor(private http: HttpClient) {}

  searchLogs(filter?: AuditSearchFilter): Observable<AuditLog[]> {
    let params = new HttpParams();
    if (filter) {
      if (filter.action) params = params.set('action', filter.action);
      if (filter.entityName) params = params.set('entityName', filter.entityName);
      if (filter.performedBy) params = params.set('performedBy', filter.performedBy);
      if (filter.fromDate) params = params.set('fromDate', filter.fromDate);
      if (filter.toDate) params = params.set('toDate', filter.toDate);
      if (filter.page != null) params = params.set('page', filter.page.toString());
      if (filter.size != null) params = params.set('size', filter.size.toString());
    }
    return this.http.get<AuditLog[]>(this.baseUrl, { params });
  }

  getStats(): Observable<AuditStats> {
    return this.http.get<AuditStats>(`${this.baseUrl}/stats`);
  }
}
