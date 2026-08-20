export interface Variable {
  id: string;
  nomVariable: string;
  type: 'STRING' | 'FLOAT' | 'DATE' | 'BOOLEAN' | 'IMAGE' | 'ARRAY' | 'CALCULEE';
  obligatoire: boolean;
  description: string;
}