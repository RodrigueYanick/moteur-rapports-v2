import { Variable } from './variable.model';

export interface TemplateSchema {
  templateId: string;
  nom: string;
  version: number;
  variables: Variable[];
}
