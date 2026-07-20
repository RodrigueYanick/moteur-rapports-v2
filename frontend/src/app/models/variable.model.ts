export interface Variable {
    id: string;
    nomVariable: string
    type: 'STRING' | 'FLOAT' | 'DATE' | 'BOOLEAN' | 'ARRAY';
    obligatoire: boolean;
}