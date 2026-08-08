export interface Template{
    id: string;
    nom: string;
    description?: string;
    contenuDesign?: any | null;
    statut: 'BROUILLON' | 'PUBLIE' | 'ARCHIVE';
    version: number;
    dateCreation: string;
    dateModification: string;
    categorie?: string;
    formatPapier?: string;
}