import { DesignBlock } from '../../models/design-block.model';
import { BlockHtmlRenderer, RenderContext } from './block-html-renderer.interface';

export class ShapeBlockRenderer implements BlockHtmlRenderer {
  supports(type: string): boolean {
    return type === 'rectangle' || type === 'cercle';
  }

  render(block: DesignBlock, _context: RenderContext): string {
    const fill = block.style?.fill || '#e5e7eb';
    const couleur = block.style?.couleur || '#94a3b8';
    const epaisseur = block.style?.epaisseur ?? 1;
    const radius = block.type === 'cercle' ? '50%' : `${block.style?.borderRadius ?? 0}px`;
    return `<div style="width:100%; height:100%; min-width:10px; min-height:10px; background:${fill}; border:${epaisseur}px solid ${couleur}; border-radius:${radius}; box-sizing:border-box;"></div>`;
  }
}

