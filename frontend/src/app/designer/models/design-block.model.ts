export interface DesignBlock {
  id: string;
  type: 'titre' | 'texte' | 'tableau' | 'ligne' | 'image'
      | 'rectangle' | 'cercle' | 'qrcode' | 'codebarre' | 'signature' | 'graphique';
  contenu?: string;
  style?: {
    fontSize?: number;
    bold?: boolean;
    italic?: boolean;
    underline?: boolean;
    align?: string;
    verticalAlign?: string;
    color?: string;
    fontFamily?: string;
    epaisseur?: number;
    couleur?: string;
    largeur?: number;
    // Nouveaux : formes
    fill?: string;
    borderRadius?: number;
    background_color?: string;
    border?: string
  };
  x?: number;
  y?: number;
  visible?: boolean;
  locked?: boolean;
  url?: string;
  lignes?: string[][];
  source?: string;
  colonnes?: { titre: string; variable: string }[];
  nom?: string;
  largeurBox?: number;
  hauteurBox?: number;
  opacite?: number;
  dataBinding?: {
    format?: string;
    valeurDefaut?: string;
  };
}