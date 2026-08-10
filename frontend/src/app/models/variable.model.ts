export interface Variable {
  id: string;
  nomVariable: string;
  type: 'STRING' | 'FLOAT' | 'DATE' | 'BOOLEAN' | 'IMAGE' | 'ARRAY';
  obligatoire: boolean;
  description: string;
}
