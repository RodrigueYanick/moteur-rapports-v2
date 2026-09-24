import { DesignBlock } from '../../models/design-block.model';
import { BlockHtmlRenderer, RenderContext } from './block-html-renderer.interface';

export class TextBlockRenderer implements BlockHtmlRenderer {
  supports(type: string): boolean {
    return type === 'titre' || type === 'texte' || type === 'ligne';
  }

  render(block: DesignBlock, context: RenderContext): string {
    if (block.type === 'ligne') {
      const epaisseur = block.style?.epaisseur || 1;
      const couleur = block.style?.couleur || '#000';
      const largeur = block.style?.largeur || 100;
      return `<div style="height:${epaisseur}px; background-color:${couleur}; width:${largeur}%;"></div>`;
    }

    const tag = block.type === 'titre' ? 'h1' : 'p';
    const style = `font-size:${block.style?.fontSize || 12}px; font-weight:${block.style?.bold ? 'bold' : 'normal'}; font-style:${block.style?.italic ? 'italic' : 'normal'}; text-decoration:${block.style?.underline ? 'underline' : 'none'}; text-align:${block.style?.align || 'left'}; color:${block.style?.color || '#000000'}; font-family:${block.style?.fontFamily || 'inherit'}; margin:0;`;
    return `<${tag} style="${style}">${context.replaceVars(block.contenu || '', context.mockData)}</${tag}>`;
  }
}

