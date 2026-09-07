export interface Template {
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
  modePagination?: 'FIXED' | 'AUTO';
  largeurMm?: number;
  hauteurMm?: number;
  margeHautMm?: number;
  margeBasMm?: number;
  margeGaucheMm?: number;
  margeDroiteMm?: number;
  parentTemplateId?: string | null;
}
