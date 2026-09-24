import { Injectable, signal, computed } from '@angular/core';
import { BLOCK_DEFAULT_DIMENSIONS, DesignBlock } from '../models/design-block.model';
import { UsableArea } from './designer-geometry.service';

export type AlignmentType = 'left' | 'centerH' | 'right' | 'top' | 'centerV' | 'bottom';
export type DistributionAxis = 'horizontal' | 'vertical';

@Injectable({
  providedIn: 'root',
})
export class DesignerSelectionService {
  private readonly _selectedBlockIds = signal<string[]>([]);
  private readonly _selectedBlock = signal<DesignBlock | null>(null);

  readonly selectedBlockIds = this._selectedBlockIds.asReadonly();
  readonly selectedBlock = this._selectedBlock.asReadonly();
  readonly hasSelection = computed(() => this._selectedBlockIds().length > 0);
  readonly isMultiSelection = computed(() => this._selectedBlockIds().length > 1);

  /**
   * Sélectionne un bloc, avec prise en charge de la multi-sélection.
   */
  select(block: DesignBlock | null, multi = false, currentBlocks: DesignBlock[] = []): void {
    if (!block) {
      this.clear();
      return;
    }

    if (multi) {
      const currentIds = this._selectedBlockIds();
      if (currentIds.includes(block.id)) {
        const nextIds = currentIds.filter(id => id !== block.id);
        this._selectedBlockIds.set(nextIds);
        this._selectedBlock.set(nextIds.length > 0 ? (currentBlocks.find(b => b.id === nextIds[nextIds.length - 1]) || null) : null);
      } else {
        const nextIds = [...currentIds, block.id];
        this._selectedBlockIds.set(nextIds);
        this._selectedBlock.set(block);
      }
    } else {
      this._selectedBlockIds.set([block.id]);
      this._selectedBlock.set(block);
    }
  }

  /**
   * Met à jour la liste des IDs sélectionnés (émis par le canvas par exemple).
   */
  setSelectionIds(ids: string[], currentBlocks: DesignBlock[]): void {
    this._selectedBlockIds.set(ids);
    if (ids.length === 1) {
      this._selectedBlock.set(currentBlocks.find(b => b.id === ids[0]) || null);
    } else if (ids.length === 0) {
      this._selectedBlock.set(null);
    } else {
      this._selectedBlock.set(currentBlocks.find(b => b.id === ids[ids.length - 1]) || null);
    }
  }

  /**
   * Réinitialise toute sélection.
   */
  clear(): void {
    this._selectedBlockIds.set([]);
    this._selectedBlock.set(null);
  }

  /**
   * Vérifie si un bloc est sélectionné.
   */
  isSelected(blockId: string): boolean {
    return this._selectedBlockIds().includes(blockId);
  }

  /**
   * Met à jour la référence de l'objet sélectionné si ses propriétés ont changé.
   */
  updateSelectedBlock(block: DesignBlock | null): void {
    this._selectedBlock.set(block);
  }

  /**
   * Aligne les blocs sélectionnés selon le type spécifié.
   */
  alignSelected(
    blocks: DesignBlock[],
    type: AlignmentType,
    clampCallback: (b: DesignBlock) => void
  ): boolean {
    const selectedIds = this._selectedBlockIds();
    if (selectedIds.length < 2) return false;

    const targetBlocks = blocks.filter(b => selectedIds.includes(b.id));
    if (targetBlocks.length < 2) return false;

    const dims = (b: DesignBlock) => {
      const fb = BLOCK_DEFAULT_DIMENSIONS[b.type] || { w: 200, h: 50 };
      return { w: b.largeurBox || fb.w, h: b.hauteurBox || fb.h };
    };

    let minX = Infinity;
    let maxX = -Infinity;
    let minY = Infinity;
    let maxY = -Infinity;

    targetBlocks.forEach(b => {
      const { w, h } = dims(b);
      const bx = b.x || 0;
      const by = b.y || 0;
      minX = Math.min(minX, bx);
      maxX = Math.max(maxX, bx + w);
      minY = Math.min(minY, by);
      maxY = Math.max(maxY, by + h);
    });

    const centerX = Math.round((minX + maxX) / 2);
    const centerY = Math.round((minY + maxY) / 2);

    targetBlocks.forEach(b => {
      const { w, h } = dims(b);
      switch (type) {
        case 'left':
          b.x = minX;
          break;
        case 'centerH':
          b.x = Math.round(centerX - w / 2);
          break;
        case 'right':
          b.x = maxX - w;
          break;
        case 'top':
          b.y = minY;
          break;
        case 'centerV':
          b.y = Math.round(centerY - h / 2);
          break;
        case 'bottom':
          b.y = maxY - h;
          break;
      }
      clampCallback(b);
    });

    return true;
  }

  /**
   * Distribue uniformément les blocs sélectionnés sur l'axe spécifié.
   */
  distributeSelected(
    blocks: DesignBlock[],
    axis: DistributionAxis,
    clampCallback: (b: DesignBlock) => void
  ): boolean {
    const selectedIds = this._selectedBlockIds();
    if (selectedIds.length < 3) return false;

    const targetBlocks = blocks.filter(b => selectedIds.includes(b.id));
    if (targetBlocks.length < 3) return false;

    if (axis === 'horizontal') {
      targetBlocks.sort((a, b) => (a.x || 0) - (b.x || 0));
      const first = targetBlocks[0];
      const last = targetBlocks[targetBlocks.length - 1];
      const totalDistance = (last.x || 0) - (first.x || 0);
      const step = totalDistance / (targetBlocks.length - 1);

      targetBlocks.forEach((b, i) => {
        if (i > 0 && i < targetBlocks.length - 1) {
          b.x = Math.round((first.x || 0) + i * step);
          clampCallback(b);
        }
      });
    } else {
      targetBlocks.sort((a, b) => (a.y || 0) - (b.y || 0));
      const first = targetBlocks[0];
      const last = targetBlocks[targetBlocks.length - 1];
      const totalDistance = (last.y || 0) - (first.y || 0);
      const step = totalDistance / (targetBlocks.length - 1);

      targetBlocks.forEach((b, i) => {
        if (i > 0 && i < targetBlocks.length - 1) {
          b.y = Math.round((first.y || 0) + i * step);
          clampCallback(b);
        }
      });
    }

    return true;
  }
}

