import { DesignBlock } from '../../models/design-block.model';
import { BlockHtmlRenderer, RenderContext } from './block-html-renderer.interface';

export class ChartBlockRenderer implements BlockHtmlRenderer {
  supports(type: string): boolean {
    return type === 'graphique';
  }

  render(block: DesignBlock, context: RenderContext): string {
    const source = (block.source || '').replace('{{', '').replace('}}', '').trim();
    const items = context.mockData[source];
    const w = block.largeurBox || 300;
    const h = block.hauteurBox || 180;
    if (!Array.isArray(items) || items.length === 0) {
      return `<table style="width:100%; height:100%; border:1px dashed #ccc; border-collapse:collapse; margin:0; padding:0; box-sizing:border-box;"><tr><td style="vertical-align:middle; text-align:center; color:#999; font-size:11px; font-family:Arial,sans-serif;">Graphique (${context.escape(source)})</td></tr></table>`;
    }
    const max = Math.max(...items.map((i: any) => Number(i.value) || 0), 1);
    const barColor = block.style?.fill || block.style?.couleur || block.style?.color || '#6d5efc';
    const paddingTotalH = 16;
    const labelRowHeight = 22;
    const barAreaHeight = Math.max(20, h - paddingTotalH - labelRowHeight);

    let barsRow = '';
    let labelsRow = '';
    for (const item of items) {
      const value = Number(item.value) || 0;
      const barHeight = Math.max(2, (value / max) * barAreaHeight);
      const valStr = Number.isInteger(value) ? String(value) : value.toFixed(1);
      barsRow += `<td style="vertical-align:bottom; text-align:center; padding:0 2px;">
        <div style="font-size:8px; color:#777; margin-bottom:2px;">${valStr}</div>
        <div style="width:70%; margin:0 auto; background:${context.escape(barColor)}; height:${barHeight}px; border-radius:3px 3px 0 0;"></div>
      </td>`;
      labelsRow += `<td style="vertical-align:top; text-align:center; font-size:9px; color:#555; padding-top:4px; white-space:nowrap; overflow:hidden;">${context.escape(String(item.label))}</td>`;
    }

    return `<div style="width:100%; height:100%; padding:8px; box-sizing:border-box; border-left:1px solid #ccc; border-bottom:1px solid #ccc; font-family:Arial,sans-serif;">
      <table style="width:100%; height:100%; table-layout:fixed; border-collapse:collapse; border:none; margin:0; padding:0;">
        <tr style="height:${barAreaHeight}px;">${barsRow}</tr>
        <tr style="height:${labelRowHeight}px;">${labelsRow}</tr>
      </table>
    </div>`;
  }
}

