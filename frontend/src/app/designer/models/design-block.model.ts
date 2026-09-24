export interface TableCell {
  value: string;
  bgColor?: string;
  textColor?: string;
  colSpan?: number;   // nombre de colonnes fusionnées (par défaut 1)
  rowSpan?: number;   // nombre de lignes fusionnées (par défaut 1)
  hidden?: boolean;   // true si la cellule est absorbée par une fusion
}

export type ConditionOperator =
  | 'EQUALS'
  | 'NOT_EQUALS'
  | 'GREATER_THAN'
  | 'GREATER_OR_EQUAL'
  | 'LESS_THAN'
  | 'LESS_OR_EQUAL'
  | 'CONTAINS'
  | 'STARTS_WITH'
  | 'IS_EMPTY'
  | 'IS_NOT_EMPTY';

export interface ConditionalStyleEffect {
  color?: string;
  backgroundColor?: string;
  bold?: boolean;
  italic?: boolean;
  underline?: boolean;
  badgeStyle?: 'NONE' | 'SUCCESS' | 'WARNING' | 'DANGER' | 'INFO';
}

export interface ConditionalStyleRule {
  id: string;
  champ: string;                // Nom de la variable ou colonne (ex: "solde", "statut")
  operateur: ConditionOperator;   // Opérateur de comparaison
  valeur: any;                  // Valeur cible de comparaison
  effet: ConditionalStyleEffect; // Styles à appliquer si la condition est remplie
}

export interface TableColumn {
  titre: string;
  variable: string;
  formule?: string;
  agregat?: 'NONE' | 'SUM' | 'AVG' | 'COUNT' | 'MIN' | 'MAX';
  conditionalStyles?: ConditionalStyleRule[];
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
  condition?: string;
  showIf?: string;
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
  colonnes?: TableColumn[];
  groupBy?: string;
  groupHeaderTemplate?: string;
  afficherSousTotaux?: boolean;
  graphiqueType?: 'bar' | 'line' | 'pie' | 'donut';
  graphiqueLabelKey?: string;
  graphiqueValueKey?: string;
  graphiquePalette?: string[];
  nom?: string;
  largeurBox?: number;
  hauteurBox?: number;
  opacite?: number;
  dataBinding?: {
    format?: string;
    valeurDefaut?: string;
  };
  conditionalStyles?: ConditionalStyleRule[];
  repeterEnTeteChaquePage?: boolean;
  eviterCoupureLignes?: boolean;
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