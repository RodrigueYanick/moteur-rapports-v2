import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ScheduledJobRequest,
  ScheduledJobResponse,
  ScheduledJobExecutionResponse
} from '../models/scheduled-job.model';

@Injectable({
  providedIn: 'root'
})
export class ScheduledJobService {
  private baseUrl = '/api/schedules';

  constructor(private http: HttpClient) {}

  getAll(): Observable<ScheduledJobResponse[]> {
    return this.http.get<ScheduledJobResponse[]>(this.baseUrl);
  }

  getById(id: string): Observable<ScheduledJobResponse> {
    return this.http.get<ScheduledJobResponse>(`${this.baseUrl}/${id}`);
  }

  create(job: ScheduledJobRequest): Observable<ScheduledJobResponse> {
    return this.http.post<ScheduledJobResponse>(this.baseUrl, job);
  }

  update(id: string, job: ScheduledJobRequest): Observable<ScheduledJobResponse> {
    return this.http.put<ScheduledJobResponse>(`${this.baseUrl}/${id}`, job);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  runNow(id: string): Observable<ScheduledJobExecutionResponse> {
    return this.http.post<ScheduledJobExecutionResponse>(`${this.baseUrl}/${id}/run-now`, {});
  }

  getExecutions(id: string): Observable<ScheduledJobExecutionResponse[]> {
    return this.http.get<ScheduledJobExecutionResponse[]>(`${this.baseUrl}/${id}/executions`);
  }
}
