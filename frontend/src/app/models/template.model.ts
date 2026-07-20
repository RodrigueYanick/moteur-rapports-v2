export interface Template{
    id: string;
    nom: string;
    description?: string;
    contentDesigne: any;
    statut: 'BROUILLON' | 'PUBLIE' | 'ARCHIVE';
    version: number;
    dateCreation: string;
    dateModification: string;
}