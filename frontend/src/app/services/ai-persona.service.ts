import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AiPersonaPromptRequest {
  personaType: string;
  userPrompt: string;
  targetLanguage?: string;
  includeMockData?: boolean;
}

export interface AiPersonaGenerationResponse {
  personaType: string;
  nom: string;
  description: string;
  categorie: string;
  formatPapier: string;
  contenuDesign: string;
  schema: string;
  sampleData: Record<string, any>;
  promptUsed: string;
  fromMock: boolean;
  provider: string;
}

@Injectable({
  providedIn: 'root'
})
export class AiPersonaService {
  private readonly baseUrl = '/api/ai/persona-generate';

  constructor(private http: HttpClient) {}

  generatePersonaTemplate(request: AiPersonaPromptRequest): Observable<AiPersonaGenerationResponse> {
    return this.http.post<AiPersonaGenerationResponse>(this.baseUrl, request);
  }
}

