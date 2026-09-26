import { DesignBlock } from '../../models/design-block.model';
import { BlockHtmlRenderer, RenderContext } from './block-html-renderer.interface';

export class BarcodeBlockRenderer implements BlockHtmlRenderer {
  supports(type: string): boolean {
    return type === 'qrcode' || type === 'codebarre' || type === 'image';
  }

  render(block: DesignBlock, context: RenderContext): string {
    if (block.type === 'image') {
      const url = context.replaceVars(block.url || '', context.mockData);
      const largeur = block.style?.largeur || 100;
      const align = block.style?.align || 'left';
      let imgStyle = `width:${largeur}px;`;
      if (align === 'center') imgStyle += 'display:block;margin:0 auto;';
      else if (align === 'right') imgStyle += 'display:block;margin-left:auto;';
      return `<img src="${context.escape(url)}" style="${imgStyle}" alt="aperçu" />`;
    }

    const label = block.type === 'qrcode' ? 'QR Code' : 'Code-barres';
    return `<table style="width:100%; height:100%; border:2px dashed #d1d5db; background:#f9fafb; border-collapse:collapse; margin:0; padding:0; box-sizing:border-box;"><tr><td style="vertical-align:middle; text-align:center; color:#6b7280; font-size:10px; font-family:Arial,sans-serif;">${label} (généré au PDF)</td></tr></table>`;
  }
}

