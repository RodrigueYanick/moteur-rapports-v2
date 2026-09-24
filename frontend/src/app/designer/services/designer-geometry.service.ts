import { Injectable } from '@angular/core';
import { BLOCK_DEFAULT_DIMENSIONS, DesignBlock } from '../models/design-block.model';

export interface UsableArea {
  minX: number;
  minY: number;
  maxX: number;
  maxY: number;
  width: number;
  height: number;
}

export interface PaperDimensions {
  width: number;
  height: number;
}

@Injectable({
  providedIn: 'root',
})
export class DesignerGeometryService {
  readonly pxPerMm = 96 / 25.4; // 96 DPI standard web

  private readonly standardSizes: Record<string, { wMm: number; hMm: number }> = {
    'A0': { wMm: 841, hMm: 1189 },
    'A1': { wMm: 594, hMm: 841 },
    'A2': { wMm: 420, hMm: 594 },
    'A3': { wMm: 297, hMm: 420 },
    'A4': { wMm: 210, hMm: 297 },
    'A5': { wMm: 148, hMm: 210 },
    'A6': { wMm: 105, hMm: 148 },
    'A7': { wMm: 74, hMm: 105 },
    'A8': { wMm: 52, hMm: 74 },
    'A9': { wMm: 37, hMm: 52 },
    'A10': { wMm: 26, hMm: 37 },
    'Letter': { wMm: 216, hMm: 279 },
    'Legal': { wMm: 216, hMm: 356 },
  };

  /**
   * Calcule les dimensions en pixels du canevas pour un format papier donné.
   */
  getPaperDimensions(format: string, largeurMm?: number | null, hauteurMm?: number | null): PaperDimensions {
    if (format === 'CUSTOM' && largeurMm && hauteurMm) {
      return {
        width: Math.round(largeurMm * this.pxPerMm),
        height: Math.round(hauteurMm * this.pxPerMm),
      };
    }
    const size = this.standardSizes[format] || this.standardSizes['A4'];
    return {
      width: Math.round(size.wMm * this.pxPerMm),
      height: Math.round(size.hMm * this.pxPerMm),
    };
  }

  /**
   * Calcule la zone utile (hors marges) en pixels.
   */
  getUsableArea(
    canvasWidth: number,
    canvasHeight: number,
    margeGaucheMm: number,
    margeDroiteMm: number,
    margeHautMm: number,
    margeBasMm: number
  ): UsableArea {
    const mLeftPx = Math.round(((margeGaucheMm != null && margeGaucheMm > 0) ? margeGaucheMm : 10) * this.pxPerMm);
    const mRightPx = Math.round(((margeDroiteMm != null && margeDroiteMm > 0) ? margeDroiteMm : 10) * this.pxPerMm);
    const mTopPx = Math.round(((margeHautMm != null && margeHautMm > 0) ? margeHautMm : 10) * this.pxPerMm);
    const mBottomPx = Math.round(((margeBasMm != null && margeBasMm > 0) ? margeBasMm : 10) * this.pxPerMm);

    return {
      minX: mLeftPx,
      minY: mTopPx,
      maxX: canvasWidth - mRightPx,
      maxY: canvasHeight - mBottomPx,
      width: Math.max(0, canvasWidth - mLeftPx - mRightPx),
      height: Math.max(0, canvasHeight - mTopPx - mBottomPx),
    };
  }

  /**
   * Contraint la position et les dimensions d'un bloc pour qu'il reste dans la zone utile.
   */
  clampBlockToUsableArea(block: DesignBlock, area: UsableArea): void {
    const fallback = BLOCK_DEFAULT_DIMENSIONS[block.type] || { w: 200, h: 50 };
    const w = block.largeurBox || fallback.w;
    const h = block.hauteurBox || fallback.h;

    // Limiter les dimensions si le bloc dépasse la zone utile
    const maxW = area.width;
    const maxH = area.height;
    if (w > maxW && maxW > 0) block.largeurBox = maxW;
    if (h > maxH && maxH > 0) block.hauteurBox = maxH;

    const effectiveW = block.largeurBox || fallback.w;
    const effectiveH = block.hauteurBox || fallback.h;

    // Contrainte horizontale
    block.x = Math.max(area.minX, Math.min(block.x || 0, Math.max(area.minX, area.maxX - effectiveW)));

    // Pour les tableaux, ils peuvent s'étendre verticalement sur plusieurs pages
    if (block.type === 'tableau') {
      block.y = Math.max(area.minY, block.y || 0);
    } else {
      block.y = Math.max(area.minY, Math.min(block.y || 0, Math.max(area.minY, area.maxY - effectiveH)));
    }
  }

  /**
   * Calcule la hauteur virtuelle du canvas pour le mode pagination automatique.
   */
  getVirtualCanvasHeight(blocks: DesignBlock[], defaultHeight: number, isAutoPagination: boolean): number {
    if (!isAutoPagination) {
      return defaultHeight;
    }
    let maxY = 0;
    for (const block of blocks) {
      const bottom = (block.y || 0) + (block.hauteurBox || 100);
      if (bottom > maxY) maxY = bottom;
    }
    return Math.max(defaultHeight, maxY + 200);
  }
}

