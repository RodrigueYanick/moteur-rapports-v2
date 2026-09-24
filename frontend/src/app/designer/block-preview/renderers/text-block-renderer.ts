import { DesignBlock } from '../../models/design-block.model';
import { BlockHtmlRenderer, RenderContext } from './block-html-renderer.interface';
import { ConditionEvaluatorService } from '../../services/condition-evaluator.service';

function getBadgeInlineStyle(badgeStyle: string): string {
  switch (badgeStyle) {
    case 'SUCCESS':
      return 'background-color:#dcfce7;color:#15803d;font-weight:600;border:1px solid #86efac;';
    case 'WARNING':
      return 'background-color:#fef3c7;color:#b45309;font-weight:600;border:1px solid #fde68a;';
    case 'DANGER':
      return 'background-color:#fee2e2;color:#b91c1c;font-weight:600;border:1px solid #fca5a5;';
    case 'INFO':
      return 'background-color:#dbeafe;color:#1d4ed8;font-weight:600;border:1px solid #93c5fd;';
    default:
      return '';
  }
}

export class TextBlockRenderer implements BlockHtmlRenderer {
  private evaluator = new ConditionEvaluatorService();

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

    const effect = this.evaluator.resolveStyles(block.conditionalStyles, context.mockData);

    const baseColor = effect?.color || block.style?.color || '#000000';
    const baseBg = effect?.backgroundColor ? `background-color:${effect.backgroundColor};` : '';
    const isBold = effect?.bold ?? (block.style?.bold || false);
    const isItalic = effect?.italic ?? (block.style?.italic || false);
    const isUnderline = effect?.underline ?? (block.style?.underline || false);

    let content = context.replaceVars(block.contenu || '', context.mockData);

    if (effect?.badgeStyle && effect.badgeStyle !== 'NONE') {
      const badgeStyle = getBadgeInlineStyle(effect.badgeStyle);
      content = `<span class="badge badge-${effect.badgeStyle.toLowerCase()}" style="display:inline-block;padding:2px 8px;border-radius:4px;font-size:0.85em;${badgeStyle}">${content}</span>`;
    }

    const tag = block.type === 'titre' ? 'h1' : 'p';
    const style = `font-size:${block.style?.fontSize || 12}px; font-weight:${isBold ? 'bold' : 'normal'}; font-style:${isItalic ? 'italic' : 'normal'}; text-decoration:${isUnderline ? 'underline' : 'none'}; text-align:${block.style?.align || 'left'}; color:${baseColor}; ${baseBg} font-family:${block.style?.fontFamily || 'inherit'}; margin:0;`;
    return `<${tag} style="${style}">${content}</${tag}>`;
  }
}


