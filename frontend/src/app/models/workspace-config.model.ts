export interface WorkspaceConfig {
  id?: string;
  codeEntreprise: string;
  formatPapier: string;
  largeurMm?: number | null;
  hauteurMm?: number | null;
  modePagination: 'FIXED' | 'AUTO';
  margeGaucheMm: number;
  margeDroiteMm: number;
  margeHautMm: number;
  margeBasMm: number;
  couleurFond: string;

  headerActif: boolean;
  hauteurHeaderMm: number;
  headerContenu?: string | null;
  headerAlignement: 'GAUCHE' | 'CENTRE' | 'DROITE';
  headerAfficherSurPremierePage: boolean;
  headerLigneSeparation: boolean;
  headerCouleurLigne: string;

  footerActif: boolean;
  hauteurFooterMm: number;
  footerContenu?: string | null;
  footerAlignement: 'GAUCHE' | 'CENTRE' | 'DROITE';
  footerAfficherSurPremierePage: boolean;
  footerLigneSeparation: boolean;
  footerCouleurLigne: string;
  numerotationPage: boolean;
  formatNumerotation: 'PAGE_X_SUR_Y' | 'PAGE_X';
}

