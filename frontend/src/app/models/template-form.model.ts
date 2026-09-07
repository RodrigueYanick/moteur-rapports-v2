export interface TemplateForm {
  nom: string;
  description?: string;
  contenuDesign?: any;
  categorie?: string;
  formatPapier?: string;
  modePagination?: 'FIXED' | 'AUTO';
  largeurMm?: number | null;
  hauteurMm?: number | null;
  margeHautMm?: number | null;
  margeBasMm?: number | null;
  margeGaucheMm?: number | null;
  margeDroiteMm?: number | null;
}
