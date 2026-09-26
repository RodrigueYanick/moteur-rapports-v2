export interface WatermarkConfig {
  actif: boolean;
  texte?: string;
  type?: 'TEXTE' | 'IMAGE';
  imageUrl?: string;
  rotation?: number; // Défaut: -45
  opacite?: number; // 0 à 100, défaut: 15
  couleur?: string; // Défaut: '#94a3b8'
  fontSize?: number; // Défaut: 54
  afficherSur?: 'TOUTES' | 'PREMIERE_PAGE' | 'SAUF_PREMIERE_PAGE';
}

