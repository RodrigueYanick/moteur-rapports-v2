import { DesignBlock } from '../../models/design-block.model';
import { BlockHtmlRenderer, RenderContext } from './block-html-renderer.interface';

export class SignatureBlockRenderer implements BlockHtmlRenderer {
  supports(type: string): boolean {
    return type === 'signature';
  }

  render(block: DesignBlock, context: RenderContext): string {
    const config = block.signatureConfig;
    const w = block.largeurBox || 220;
    const h = block.hauteurBox || 110;

    // Fallback rétrocompatible si aucun signatureConfig n'est présent
    if (!config) {
      if (block.url) {
        const url = context.replaceVars(block.url, context.mockData);
        return `<img src="${context.escape(url)}" style="width:${w}px;height:${h}px;object-fit:contain;" alt="Signature" />`;
      }
      return `<table style="width:100%;height:100%;border-bottom:1px solid #333;border-collapse:collapse;margin:0;padding:0;box-sizing:border-box;"><tr><td style="vertical-align:bottom;text-align:center;padding-bottom:4px;font-family:cursive;color:#999;font-size:12px;">Signature</td></tr></table>`;
    }

    const mention = context.replaceVars(config.mentionLegale ?? 'Lu et approuvé, bon pour accord', context.mockData);
    const signataire = context.replaceVars(config.signataireNom ?? '', context.mockData);
    const qualite = context.replaceVars(config.signataireQualite ?? '', context.mockData);
    const date = context.replaceVars(config.dateSignature ?? '', context.mockData);
    const mode = config.modeSignature || 'MANUSCRITE';

    let imageUrl = config.signatureImageUrl || block.url || '';
    if (imageUrl) {
      imageUrl = context.replaceVars(imageUrl, context.mockData);
    }

    const afficherCadre = config.afficherCadre ?? true;
    const cadrePointille = config.cadrePointille ?? true;

    let borderStyle = 'border:none;';
    if (afficherCadre) {
      const borderType = cadrePointille ? 'dashed' : 'solid';
      borderStyle = `border:1px ${borderType} #94a3b8;background-color:#f8fafc;`;
    }

    let middleContent = '';
    if (imageUrl && (mode === 'IMAGE' || mode === 'MANUSCRITE')) {
      middleContent = `<img src="${context.escape(imageUrl)}" style="max-width:${w - 20}px;max-height:${Math.max(30, h - 55)}px;object-fit:contain;" alt="Signature" />`;
    } else if (mode === 'MANUSCRITE') {
      middleContent = `<span style="font-family:cursive;font-size:14px;color:#0284c7;">✍️ Signature manuscrite</span>`;
    } else {
      middleContent = `<div style="margin:10px auto 0 auto;width:80%;border-bottom:1px dashed #94a3b8;height:10px;"></div>`;
    }

    let footerHtml = '';
    if (signataire || qualite || date) {
      footerHtml = `<tr><td style="padding:2px 8px 4px 8px;border-top:1px solid #e2e8f0;vertical-align:bottom;height:20px;">
        <table style="width:100%;border-collapse:collapse;margin:0;padding:0;"><tr>
          <td style="text-align:left;font-size:9px;color:#334155;font-weight:bold;">
            ${signataire ? context.escape(signataire) : ''}
            ${qualite ? `<span style="font-weight:normal;color:#64748b;"> (${context.escape(qualite)})</span>` : ''}
          </td>
          ${date ? `<td style="text-align:right;font-size:8px;color:#64748b;">${context.escape(date)}</td>` : ''}
        </tr></table>
      </td></tr>`;
    }

    return `<table class="signature-block-preview" style="width:100%;height:100%;${borderStyle}border-collapse:collapse;margin:0;padding:0;box-sizing:border-box;font-family:Helvetica,Arial,sans-serif;">
      ${mention ? `<tr><td style="padding:4px 8px;font-size:9px;font-style:italic;color:#64748b;text-align:left;vertical-align:top;height:16px;">${context.escape(mention)}</td></tr>` : ''}
      <tr><td style="vertical-align:middle;text-align:center;padding:4px 8px;">
        ${middleContent}
      </td></tr>
      ${footerHtml}
    </table>`;
  }
}
