import { Injectable } from '@angular/core';
import { DesignBlock, TableCell } from '../models/design-block.model';

@Injectable({ providedIn: 'root' })
export class MockDataService {
  generate(blocks: DesignBlock[]): Record<string, any> {
    const data: Record<string, any> = {};

    for (const block of blocks) {
      // --- Cas spécial : tableau dynamique (source = variable ARRAY) ---
      if (block.type === 'tableau' && !block.lignes && block.source) {
        const varName = block.source.replace('{{', '').replace('}}', '').trim();
        if (varName && !Object.prototype.hasOwnProperty.call(data, varName)) {
          data[varName] = this.buildSampleRows(block.colonnes || []);
        }
        continue;
      }

      // Cas graphique (déjà géré)
      if (block.type === 'graphique' && block.source) {
        const varName = block.source.replace('{{', '').replace('}}', '').trim();
        if (!data[varName]) {
          data[varName] = [
            { label: 'Jan', value: 40 },
            { label: 'Fév', value: 65 },
            { label: 'Mar', value: 30 },
            { label: 'Avr', value: 80 },
          ];
        }
        continue;
      }

      // Extraction classique dans les champs texte
      const textFields = [block.contenu, block.url, block.source];
      for (const text of textFields) {
        if (text) this.extractVariables(text, data);
      }

      // Extraction dans les cellules des tableaux statiques
      if (block.lignes) {
        for (const row of block.lignes) {
          for (const cell of row) {
            const value = typeof cell === 'string' ? cell : (cell as TableCell).value;
            if (value) this.extractVariables(value, data);
          }
        }
      }

      // Extraction dans les colonnes (pour les tableaux dynamiques sans source déjà traitée)
      if (block.colonnes) {
        for (const col of block.colonnes) {
          if (col.variable) this.extractVariables(col.variable, data);
          if (col.titre) this.extractVariables(col.titre, data);
        }
      }
    }

    return data;
  }

  /**
   * Construit deux lignes d'exemple pour un tableau dynamique à partir de ses colonnes.
   */
  private buildSampleRows(colonnes: { titre: string; variable: string; formule?: string }[]): Record<string, any>[] {
    const makeRow = (i: number) => {
      const row: Record<string, any> = {};
      for (const col of colonnes) {
        if (!col.variable) continue;
        row[col.variable] = this.sampleValueForField(col.variable, i);
      }
      return row;
    };
    // 15 lignes pour que la pagination des tableaux soit visible en preview
    return Array.from({ length: 15 }, (_, i) => makeRow(i + 1));
  }

  private sampleValueForField(fieldName: string, index: number): any {
    const lower = fieldName.toLowerCase();
    if (lower.includes('qte') || lower.includes('quantite')) return index * 2;
    if (lower.includes('prix') || lower.includes('pu') || lower.includes('montant') || lower.includes('total')) return index * 15000;
    if (lower.includes('date')) return new Date().toLocaleDateString();
    return `${fieldName} ${index}`;
  }

  private extractVariables(text: string, data: Record<string, any>): void {
    const matches = text.match(/\{\{(.+?)\}\}/g);
    if (!matches) return;

    for (const m of matches) {
      const varName = m.replace(/\{\{|\}\}/g, '').trim();
      if (Object.prototype.hasOwnProperty.call(data, varName)) continue;

      if (varName.startsWith('titre')) data[varName] = 'Titre exemple';
      else if (varName.startsWith('texte')) data[varName] = 'Texte exemple';
      else if (varName.startsWith('image')) {
        data[varName] = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="150" height="150" viewBox="0 0 150 150"%3E%3Crect width="150" height="150" fill="%23eef0f4"/%3E%3Cpath d="M25 110l32-35 23 23 13-14 32 26H25z" fill="%2399a2b3"/%3E%3Ccircle cx="99" cy="50" r="14" fill="%236d5efc"/%3E%3C/svg%3E';
      }
      else if (varName.startsWith('qrcode') || varName.startsWith('codebarre')) data[varName] = 'DEMO-12345';
      else data[varName] = varName;
    }
  }
}