export interface TableCell {
  value: string;
  bgColor?: string;
  textColor?: string;
}

export interface DesignBlock {
  rotation: number;
  id: string;
  type:
    | 'titre'
    | 'texte'
    | 'tableau'
    | 'ligne'
    | 'image'
    | 'rectangle'
    | 'cercle'
    | 'qrcode'
    | 'codebarre'
    | 'signature'
    | 'graphique';
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
    fill?: string;
    borderRadius?: number;
    // --- Nouveaux : style global du tableau ---
    bordureCouleur?: string;
    texteCouleurDefaut?: string;
    rotation?: number;
  };
  x?: number;
  y?: number;
  visible?: boolean;
  locked?: boolean;
  url?: string;
  lignes?: TableCell[][];
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

export const BLOCK_DEFAULT_DIMENSIONS: Record<DesignBlock['type'], { w: number; h: number }> = {
  titre: { w: 400, h: 40 },
  texte: { w: 400, h: 40 },
  tableau: { w: 750, h: 150 },
  ligne: { w: 780, h: 10 },
  image: { w: 150, h: 150 },
  rectangle: { w: 150, h: 100 },
  cercle: { w: 100, h: 100 },
  qrcode: { w: 100, h: 100 },
  codebarre: { w: 160, h: 60 },
  signature: { w: 180, h: 70 },
  graphique: { w: 300, h: 180 },
};

export interface DesignPage {
  id: string;
  nom: string;
  blocks: DesignBlock[];
}