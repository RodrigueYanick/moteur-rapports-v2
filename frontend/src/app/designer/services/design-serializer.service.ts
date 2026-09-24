import { Injectable } from '@angular/core';
import { DesignBlock, DesignPage, TableCell } from '../models/design-block.model';

@Injectable({ providedIn: 'root' })
export class DesignSerializer {
  /** DesignBlock[] → JSON string (contenuDesign) */
  serialize(pages: DesignPage[]): string {
    const outPages = pages.map(p => ({
      nom: p.nom,
      blocs: p.blocks.map(b => this.serializeBlock(b))
    }));
    return JSON.stringify({ pages: outPages });
  }

  deserialize(json: string): DesignPage[] {
    if (!json) return [this.createEmptyPage('Page 1')];
    try {
      const root = JSON.parse(json);

      if (Array.isArray(root.pages)) {
        return root.pages.map((p: any, idx: number) => ({
          id: crypto.randomUUID(),
          nom: p.nom || `Page ${idx + 1}`,
          blocks: Array.isArray(p.blocs) ? p.blocs.map((b: any) => this.deserializeBlock(b)) : []
        }));
      }

      // Rétrocompatibilité : ancien format à plat { blocs: [...] }
      if (Array.isArray(root.blocs)) {
        return [{
          id: crypto.randomUUID(),
          nom: 'Page 1',
          blocks: root.blocs.map((b: any) => this.deserializeBlock(b))
        }];
      }

      return [this.createEmptyPage('Page 1')];
    } catch {
      return [this.createEmptyPage('Page 1')];
    }
  }

  createEmptyPage(nom: string): DesignPage {
    return { id: crypto.randomUUID(), nom, blocks: [] };
  }

  // ============================================================
  // SÉRIALISATION
  // ============================================================

  private serializeBlock(block: DesignBlock): any {
    const b: any = { type: block.type };
    if (block.rotation !== undefined) b.rotation = block.rotation;

    // --- Champs communs à tous les types ---
    b.x = block.x ?? 0;
    b.y = block.y ?? 0;
    if (block.nom !== undefined) b.nom = block.nom;
    if (block.largeurBox !== undefined) b.largeurBox = block.largeurBox;
    if (block.hauteurBox !== undefined) b.hauteurBox = block.hauteurBox;
    if (block.opacite !== undefined) b.opacite = block.opacite;
    if (block.visible !== undefined) b.visible = block.visible;
    if (block.locked !== undefined) b.locked = block.locked;
    if (block.dataBinding !== undefined) {
      b.dataBinding = {
        format: block.dataBinding.format ?? '',
        valeurDefaut: block.dataBinding.valeurDefaut ?? '',
      };
    }

    // --- Champs spécifiques au type ---
    switch (block.type) {
      case 'titre':
      case 'texte':
        b.contenu = block.contenu || '';
        b.style = {
          fontSize: block.style?.fontSize ?? 12,
          bold: block.style?.bold ?? false,
          italic: block.style?.italic ?? false,
          underline: block.style?.underline ?? false,
          align: block.style?.align ?? 'left',
          verticalAlign: block.style?.verticalAlign ?? 'top',
          color: block.style?.color ?? '#000000',
          fontFamily: block.style?.fontFamily ?? 'Inter',
        };
        break;

      case 'ligne':
        b.style = {
          epaisseur: block.style?.epaisseur ?? 1,
          couleur: block.style?.couleur ?? '#000000',
          largeur: block.style?.largeur ?? 100,
        };
        break;

      case 'tableau':
        if (block.lignes) {
          b.lignes = block.lignes;
        } else {
          b.source = block.source || '';
          b.colonnes = block.colonnes || [];
          if (block.groupBy) b.groupBy = block.groupBy;
          if (block.groupHeaderTemplate) b.groupHeaderTemplate = block.groupHeaderTemplate;
          if (block.afficherSousTotaux !== undefined) b.afficherSousTotaux = block.afficherSousTotaux;
        }
        b.style = {
          bordureCouleur: block.style?.bordureCouleur ?? '#d9d9d9',
          texteCouleurDefaut: block.style?.texteCouleurDefaut ?? '#000000',
        };
        break;

      case 'image':
        b.url = block.url || '';
        b.style = {
          largeur: block.style?.largeur ?? 100,
          alignement: block.style?.align ?? 'left',
        };
        break;

      case 'rectangle':
      case 'cercle':
        b.style = {
          fill: block.style?.fill ?? '#e5e7eb',
          couleur: block.style?.couleur ?? '#94a3b8',
          epaisseur: block.style?.epaisseur ?? 1,
          borderRadius: block.style?.borderRadius ?? 0,
        };
        break;

      case 'qrcode':
      case 'codebarre':
        b.url = block.url || '';
        break;

      case 'signature':
        // Pas de champ spécifique pour l'instant (largeurBox/hauteurBox suffisent)
        break;

      case 'graphique':
        b.source = block.source || '';
        b.graphiqueType = block.graphiqueType || 'bar';
        if (block.style) b.style = { ...block.style };
        break;
    }

    return b;
  }

  // ============================================================
  // DÉSÉRIALISATION
  // ============================================================

  private deserializeBlock(b: any): DesignBlock {
    const block: DesignBlock = {
      id: crypto.randomUUID(),
      type: b.type,
      rotation: b.rotation ?? 0,
      style: {},
    };

    // --- Champs communs à tous les types ---
    block.x = b.x ?? 0;
    block.y = b.y ?? 0;
    if (b.nom !== undefined) block.nom = b.nom;
    if (b.largeurBox !== undefined) block.largeurBox = b.largeurBox;
    if (b.hauteurBox !== undefined) block.hauteurBox = b.hauteurBox;
    if (b.opacite !== undefined) block.opacite = b.opacite;
    block.visible = b.visible ?? true;
    block.locked = b.locked ?? false;
    if (b.dataBinding !== undefined) {
      block.dataBinding = {
        format: b.dataBinding.format ?? '',
        valeurDefaut: b.dataBinding.valeurDefaut ?? '',
      };
    }

    // --- Champs spécifiques au type ---
    switch (b.type) {
      case 'titre':
      case 'texte':
        block.contenu = b.contenu || '';
        block.style = {
          fontSize: b.style?.fontSize ?? 12,
          bold: b.style?.bold ?? false,
          italic: b.style?.italic ?? false,
          underline: b.style?.underline ?? false,
          align: b.style?.align ?? 'left',
          verticalAlign: b.style?.verticalAlign ?? 'top',
          color: b.style?.color ?? '#000000',
          fontFamily: b.style?.fontFamily ?? 'Inter',
        };
        break;

      case 'tableau':
        if (b.lignes) {
          block.lignes = this.migrateLignes(b.lignes);
        } else {
          block.source = b.source || '';
          block.colonnes = b.colonnes || [];
          block.groupBy = b.groupBy;
          block.groupHeaderTemplate = b.groupHeaderTemplate;
          block.afficherSousTotaux = b.afficherSousTotaux;
        }
        block.style = {
          bordureCouleur: b.style?.bordureCouleur ?? '#d9d9d9',
          texteCouleurDefaut: b.style?.texteCouleurDefaut ?? '#000000',
        };
        break;

      case 'ligne':
        block.style = {
          epaisseur: b.style?.epaisseur ?? 1,
          couleur: b.style?.couleur ?? '#000000',
          largeur: b.style?.largeur ?? 100,
        };
        break;

      case 'image':
        block.url = b.url || '';
        block.style = {
          largeur: b.style?.largeur ?? 100,
          align: b.style?.alignement ?? 'left',
        };
        break;

      case 'rectangle':
      case 'cercle':
        block.style = {
          fill: b.style?.fill ?? '#e5e7eb',
          couleur: b.style?.couleur ?? '#94a3b8',
          epaisseur: b.style?.epaisseur ?? 1,
          borderRadius: b.style?.borderRadius ?? 0,
        };
        break;

      case 'qrcode':
      case 'codebarre':
        block.url = b.url || '';
        break;

      case 'signature':
        break;

      case 'graphique':
        block.source = b.source || '';
        block.graphiqueType = b.graphiqueType || 'bar';
        if (b.style) block.style = { ...b.style };
        break;

      default:
        // Type inconnu (design corrompu ou version future) : on garde le bloc
        // tel quel plutôt que de le faire planter, pour ne pas perdre les autres blocs.
        console.warn(`DesignSerializer: type de bloc inconnu "${b.type}", conservé tel quel.`);
        break;
    }

    return block;
  }

  // --- Nouvelle méthode privée, ajoutée à la classe ---
  /** Convertit un ancien tableau string[][] en TableCell[][], ou laisse tel quel si déjà migré. */
  private migrateLignes(rawLignes: any[][]): TableCell[][] {
    return rawLignes.map((row) =>
      row.map((cell) => {
        if (typeof cell === 'string') return { value: cell };
        return {
          value: cell?.value ?? '',
          bgColor: cell?.bgColor,
          textColor: cell?.textColor,
          colSpan: cell?.colSpan ?? 1,
          rowSpan: cell?.rowSpan ?? 1,
          hidden: cell?.hidden ?? false,
        };
      }),
    );
  }
}
