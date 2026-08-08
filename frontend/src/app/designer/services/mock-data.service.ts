import { Injectable } from '@angular/core';
import { DesignBlock } from '../models/design-block.model';

@Injectable({ providedIn: 'root' })
export class MockDataService {
  /**
   * Génère un objet de données mockées en fonction des variables trouvées dans les blocs.
   */
  generate(blocks: DesignBlock[]): Record<string, any> {
    const data: Record<string, any> = {};
    for (const block of blocks) {
      // Cas spécial : graphique a directement un "source" (pas un {{...}} dans contenu)
      if (block.type === 'graphique' && block.source) {
        const varName = block.source.replace('{{', '').replace('}}', '').trim();
        if (!data[varName]) {
          data[varName] = [
            { label: 'Jan', value: 40 },
            { label: 'Fév', value: 65 },
            { label: 'Mar', value: 30 },
            { label: 'Avr', value: 80 }
          ];
        }
        continue;
      }

      // Extraire les variables des champs texte principaux
      const textFields = [block.contenu, block.url, block.source];
      for (const text of textFields) {
        if (text) {
          this.extractVariables(text, data);
        }
      }

      // Extraire les variables des cellules de tableau (lignes)
      if (block.lignes && Array.isArray(block.lignes)) {
        for (const row of block.lignes) {
          if (Array.isArray(row)) {
            for (const cell of row) {
              if (cell) {
                this.extractVariables(cell, data);
              }
            }
          }
        }
      }

      // Extraire les variables des colonnes (binding)
      if (block.colonnes && Array.isArray(block.colonnes)) {
        for (const col of block.colonnes) {
          if (col.variable) {
            this.extractVariables(col.variable, data);
          }
          if (col.titre) {
            this.extractVariables(col.titre, data);
          }
        }
      }
    }
    return data;
  }

  /**
   * Extrait les variables {{nomVariable}} d'un texte et ajoute des valeurs mockées.
   */
  private extractVariables(text: string, data: Record<string, any>): void {
    const matches = text.match(/\{\{(.+?)\}\}/g);
    if (!matches) return;

    for (const m of matches) {
      const varName = m.replace(/\{\{|\}\}/g, '').trim();
      if (Object.prototype.hasOwnProperty.call(data, varName)) continue;

      // Valeurs par défaut selon le nom de la variable
      if (varName.startsWith('titre')) {
        data[varName] = 'Titre exemple';
      } else if (varName.startsWith('texte')) {
        data[varName] = 'Texte exemple';
      } else if (varName.startsWith('image')) {
        data[varName] = 'https://via.placeholder.com/150';
      } else if (varName.startsWith('qrcode') || varName.startsWith('codebarre')) {
        data[varName] = 'DEMO-12345';
      } else if (varName.startsWith('tableau')) {
        data[varName] = [
          { col1: 'Donnée 1', col2: 'Donnée 2' },
          { col1: 'Donnée 3', col2: 'Donnée 4' }
        ];
      } else {
        data[varName] = varName; // fallback : le nom lui-même comme valeur
      }
    }
  }

}
