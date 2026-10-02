export interface ApiResponse<T> {
  message: string;
  data: T;
  timestamp: string;
}

export interface AiTemplatePromptRequest {
  prompt: string;
  nom?: string;
  categorie?: string;
  formatPapier?: string;
}

export interface AiTemplateGenerationResponse {
  templateId: string;
  nom: string;
  description: string;
  categorie: string;
  formatPapier: string;
  contenuDesign: string;
  extractedVariables: string[];
  promptUsed: string;
  fromMock: boolean;
  provider: string;
}

export interface AiVariableItem {
  nom: string;
  type?: string;
  description?: string;
}

export interface AiMockDataRequest {
  templateId?: string;
  templateNom?: string;
  variables?: AiVariableItem[];
  arrayColumns?: string[];
  rowCount?: number;
}

export interface AiMockDataResponse {
  data: Record<string, any>;
  fromAi: boolean;
  provider: string;
  message?: string;
}

export interface AiStatusResponse {
  enabled: boolean;
  available: boolean;
  provider: string;
  model: string;
}

