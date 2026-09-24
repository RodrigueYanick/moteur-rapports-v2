import { DesignBlock } from '../../models/design-block.model';
import { BlockHtmlRenderer, RenderContext } from './block-html-renderer.interface';

export class TableBlockRenderer implements BlockHtmlRenderer {
  supports(type: string): boolean {
    return type === 'tableau';
  }

  render(block: DesignBlock, context: RenderContext): string {
    const bordure = block.style?.bordureCouleur || '#d9d9d9';
    const texteDefaut = block.style?.texteCouleurDefaut || '#000';
    const slice = (block as any)._tableSlice as { startRow: number; endRow: number } | undefined;

    if (block.lignes) {
      // Tableau fixe : la première ligne est l'en-tête (toujours affiché)
      const allRows = block.lignes;
      const headerRow = allRows[0];
      const dataRows = allRows.slice(1);
      const startRow = slice ? slice.startRow : 0;
      const endRow = slice ? slice.endRow : dataRows.length;
      const visibleRows = dataRows.slice(startRow, endRow);

      let table = `<table style="border-collapse:collapse;width:100%;font-family:Arial, sans-serif;font-size:14px;">`;
      // En-tête toujours présent
      table += '<tr>';
      for (const cell of headerRow) {
        if (cell.hidden) continue;
        const bg = cell.bgColor ? `background:${cell.bgColor};` : 'background:#f5f5f6;';
        const color = `color:${cell.textColor || texteDefaut};`;
        const colspan = cell.colSpan && cell.colSpan > 1 ? ` colspan="${cell.colSpan}"` : '';
        const rowspan = cell.rowSpan && cell.rowSpan > 1 ? ` rowspan="${cell.rowSpan}"` : '';
        table += `<td${colspan}${rowspan} style="border:1px solid ${bordure};padding:3px 5px;min-width:90px;font-weight:600;${bg}${color}">${context.replaceVars(cell.value || '', context.mockData)}</td>`;
      }
      table += '</tr>';
      // Lignes de données (slicées)
      for (const row of visibleRows) {
        table += '<tr>';
        for (const cell of row) {
          if (cell.hidden) continue;
          const bg = cell.bgColor ? `background:${cell.bgColor};` : '';
          const color = `color:${cell.textColor || texteDefaut};`;
          const colspan = cell.colSpan && cell.colSpan > 1 ? ` colspan="${cell.colSpan}"` : '';
          const rowspan = cell.rowSpan && cell.rowSpan > 1 ? ` rowspan="${cell.rowSpan}"` : '';
          table += `<td${colspan}${rowspan} style="border:1px solid ${bordure};padding:3px 5px;min-width:90px;${bg}${color}">${context.replaceVars(cell.value || '', context.mockData)}</td>`;
        }
        table += '</tr>';
      }
      return table + '</table>';
    }

    // Tableau dynamique
    const source = block.source || '';
    const sourceClean = source.replace('{{', '').replace('}}', '').trim();
    const allRows: any[] = context.mockData[sourceClean] || [];

    const tableStyle = `border-collapse:collapse;width:100%;font-family:Arial, sans-serif;font-size:14px;color:${texteDefaut};font-weight:400;`;
    const cellStyle = `border:1px solid ${bordure};padding:3px 5px;min-width:90px;text-align:left;`;
    const headerStyle = cellStyle + 'background:#f5f5f5;font-weight:600;';

    let table = `<table style="${tableStyle}">`;
    // En-tête toujours présent
    table += '<tr>';
    for (const col of block.colonnes || []) {
      table += `<td style="${headerStyle}">${context.escape(col.titre || col.variable)}</td>`;
    }
    table += '</tr>';
    // Lignes de données (slicées)
    const startRow = slice ? slice.startRow : 0;
    const endRow = slice ? slice.endRow : allRows.length;
    const visibleRows = allRows.slice(startRow, endRow);
    for (const row of visibleRows) {
      table += '<tr>';
      for (const col of block.colonnes || []) {
        const val = row[col.variable];
        table += `<td style="${cellStyle}">${context.escape(String(val))}</td>`;
      }
      table += '</tr>';
    }
    return table + '</table>';
  }
}

