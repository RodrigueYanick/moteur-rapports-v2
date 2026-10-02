import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  ApiResponse,
  AiTemplatePromptRequest,
  AiTemplateGenerationResponse,
  AiMockDataRequest,
  AiMockDataResponse,
  AiStatusResponse,
} from '../models/ai.model';

@Injectable({
  providedIn: 'root',
})
export class AiAssistantService {
  private readonly baseUrl = '/api/ai';

  constructor(private http: HttpClient) {}

  generateTemplate(request: AiTemplatePromptRequest): Observable<AiTemplateGenerationResponse> {
    return this.http
      .post<ApiResponse<AiTemplateGenerationResponse>>(`${this.baseUrl}/generate-template`, request)
      .pipe(map((res) => res.data));
  }

  generateMockData(request: AiMockDataRequest): Observable<AiMockDataResponse> {
    return this.http
      .post<ApiResponse<AiMockDataResponse>>(`${this.baseUrl}/mock-data`, request)
      .pipe(map((res) => res.data));
  }

  getStatus(): Observable<AiStatusResponse> {
    return this.http
      .get<ApiResponse<AiStatusResponse>>(`${this.baseUrl}/status`)
      .pipe(map((res) => res.data));
  }
}

