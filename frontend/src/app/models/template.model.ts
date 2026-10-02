import { TemplateWorkflowStatus } from './workflow.model';

export type TemplateStatut = TemplateWorkflowStatus;

export interface Template {
  id: string;
  nom: string;
  description?: string;
  contenuDesign?: any | null;
  statut: TemplateWorkflowStatus;
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
  couleurFond?: string;
  headerActif?: boolean;
  hauteurHeaderMm?: number;
  headerContenu?: string | null;
  headerAlignement?: 'GAUCHE' | 'CENTRE' | 'DROITE';
  headerAfficherSurPremierePage?: boolean;
  headerLigneSeparation?: boolean;
  headerCouleurLigne?: string;
  footerActif?: boolean;
  hauteurFooterMm?: number;
  footerContenu?: string | null;
  footerAlignement?: 'GAUCHE' | 'CENTRE' | 'DROITE';
  footerAfficherSurPremierePage?: boolean;
  footerLigneSeparation?: boolean;
  footerCouleurLigne?: string;
  numerotationPage?: boolean;
  formatNumerotation?: 'PAGE_X_SUR_Y' | 'PAGE_X';
  dataSourceId?: string | null;
  dataSourceNom?: string | null;
  dataSourceQuery?: string | null;
}

export interface TemplateVersionDto {
  id: string;
  nom: string;
  version: number;
  statut: TemplateWorkflowStatus;
  dateCreation: string;
  dateModification: string;
  parentTemplateId?: string | null;
  isCurrent: boolean;
  children?: TemplateVersionDto[];
}

export interface TemplateVersionTreeDto {
  rootId: string;
  currentId: string;
  totalVersions: number;
  tree: TemplateVersionDto;
  flatHistory: TemplateVersionDto[];
}

