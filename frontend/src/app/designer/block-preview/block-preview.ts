import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, Input, OnChanges, OnDestroy, OnInit, SimpleChanges, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DesignBlock, DesignPage } from '../models/design-block.model';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { MockDataService } from '../services/mock-data.service';
import { LucideAngularModule, Sparkles, Plus, Minus, RotateCcw, ArrowLeft, ArrowRight } from 'lucide-angular';
import { FillerDataService } from '../services/filler-data';
import { TemplateApiService } from '../../services/template-api';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-block-preview',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './block-preview.html',
  styleUrls: ['./block-preview.scss']
})
export class BlockPreview implements AfterViewInit, OnChanges, OnInit, OnDestroy {
  @Input() pages: DesignPage[] = [];
  @Input() canvasWidth: number = 794;
  @Input() canvasHeight: number = 1123;
  @Input() modePagination: 'FIXED' | 'AUTO' = 'FIXED';
  @Input() templateId: string | null = null;
  @Input() margeHautPx: number = 0;
  @Input() margeBasPx: number = 0;
  @Input() margeGauchePx: number = 0;
  @Input() margeDroitePx: number = 0;

  private readonly defaultMarginPx = Math.round(10 * 96 / 25.4); // ~38px

  get safeMargeHautPx(): number { return (this.margeHautPx != null && this.margeHautPx > 0) ? this.margeHautPx : this.defaultMarginPx; }
  get safeMargeBasPx(): number { return (this.margeBasPx != null && this.margeBasPx > 0) ? this.margeBasPx : this.defaultMarginPx; }
  get safeMargeGauchePx(): number { return (this.margeGauchePx != null && this.margeGauchePx > 0) ? this.margeGauchePx : this.defaultMarginPx; }
  get safeMargeDroitePx(): number { return (this.margeDroitePx != null && this.margeDroitePx > 0) ? this.margeDroitePx : this.defaultMarginPx; }

  @ViewChild('previewViewport') private previewViewport?: ElementRef<HTMLElement>;

  public previewHtml: string | null = null;
  private safePreviewHtmlValue: SafeHtml | null = null;
  safePreviewPages: { nom: string; html: SafeHtml }[] = [];
  currentPreviewPageIndex = 0;
  previewPageCount = 0;
  previewFrameHeight = 800;

  private valuesSubscription?: Subscription;
  private previewSubscription?: Subscription;
  private previewRequestTimer?: ReturnType<typeof setTimeout>;

  zoom = 0.5;
  minZoom = 0.25;
  maxZoom = 4.5;
  step = 0.05;
  private fitToWidth = true;
  private resizeObserver?: ResizeObserver;

  readonly icons = {
    preview: Sparkles,
    plus: Plus,
    minus: Minus,
    reset: RotateCcw,
    previous: ArrowLeft,
    next: ArrowRight
  };

  constructor(
    private api: TemplateApiService,
    private sanitizer: DomSanitizer,
    private mockDataService: MockDataService,
    private fillerdata: FillerDataService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngAfterViewInit(): void {
    this.updateFitZoom();
    this.resizeObserver = new ResizeObserver(() => this.updateFitZoom());
    this.resizeObserver.observe(this.previewViewport!.nativeElement);
  }

  ngOnInit(): void {
    this.valuesSubscription = this.fillerdata.values$.subscribe(() => this.refreshPreview());
  }

  ngOnDestroy(): void {
    this.valuesSubscription?.unsubscribe();
    this.previewSubscription?.unsubscribe();
    if (this.previewRequestTimer) clearTimeout(this.previewRequestTimer);
    this.resizeObserver?.disconnect();
  }

  get displayedPageCount(): number {
    if (this.modePagination === 'AUTO') return this.previewPageCount;
    return this.safePreviewPages.length;
  }

  get currentPreviewPage(): { nom: string; html: SafeHtml } | undefined {
    return this.safePreviewPages[this.currentPreviewPageIndex];
  }

  previousPreviewPage(): void {
    this.currentPreviewPageIndex = Math.max(0, this.currentPreviewPageIndex - 1);
    this.cdr.markForCheck();
  }

  nextPreviewPage(): void {
    this.currentPreviewPageIndex = Math.min(
      this.safePreviewPages.length - 1,
      this.currentPreviewPageIndex + 1
    );
    this.cdr.markForCheck();
  }

  renderPreview(): void {
    const allBlocks = this.pages.flatMap(p => p.blocks);
    const mockData = this.mockDataService.generate(allBlocks);
    const realValues = this.fillerdata.getValues();
    const mergedData = { ...mockData, ...realValues };

    const contentH = this.canvasHeight - this.safeMargeHautPx - this.safeMargeBasPx;

    // Étape 1 : générer les fragments de blocs (un bloc tableau peut produire plusieurs fragments)
    const generatedPages: { nom: string; blocks: DesignBlock[] }[] = [];
    for (const sourcePage of this.pages) {
      const pageFragments: { block: DesignBlock; pageOffset: number; tableSlice?: { startRow: number; endRow: number } }[] = [];
      for (const block of sourcePage.blocks) {
        if (block.visible === false) continue;
        const top = Math.max(0, block.y || 0);

        if (block.type === 'tableau') {
          this.paginateTableBlock(block, top, contentH, pageFragments, mergedData);
        } else {
          // Blocs non-tableau : comportement classique
          const height = block.hauteurBox ?? this.defaultDimensions(block.type).h;
          const pageOffset = Math.max(0, Math.floor((top + height - 1) / this.canvasHeight));
          const adjustedY = pageOffset === 0
            ? top
            : Math.max(this.safeMargeHautPx, top - pageOffset * this.canvasHeight);
          pageFragments.push({
            block: { ...block, y: adjustedY },
            pageOffset
          });
        }
      }

      // Étape 2 : organiser les fragments en pages
      if (pageFragments.length === 0) {
        generatedPages.push({ nom: sourcePage.nom, blocks: [] });
      } else {
        const maxPageOffset = pageFragments.reduce((max, f) => Math.max(max, f.pageOffset), 0);
        for (let pageOffset = 0; pageOffset <= maxPageOffset; pageOffset++) {
          const pageBlocks: DesignBlock[] = [];
          for (const frag of pageFragments) {
            if (frag.pageOffset !== pageOffset) continue;
            if (frag.tableSlice) {
              pageBlocks.push(this.buildSlicedTableBlock(frag.block, frag.tableSlice));
            } else {
              pageBlocks.push(frag.block);
            }
          }
          if (pageBlocks.length > 0) {
            generatedPages.push({
              nom: pageOffset === 0 ? sourcePage.nom : `${sourcePage.nom || 'Page'} ${pageOffset + 1}`,
              blocks: pageBlocks
            });
          }
        }
      }
    }

    this.safePreviewPages = generatedPages.map(page => {
      const contentW = this.canvasWidth - this.safeMargeGauchePx - this.safeMargeDroitePx;
      const contentH = this.canvasHeight - this.safeMargeHautPx - this.safeMargeBasPx;
      let html = `<div style="position:relative;width:${this.canvasWidth}px;height:${this.canvasHeight}px;background:white;overflow:hidden;margin:0 auto;">`;

      // Zones de marges interdites (hachures + fond semi-transparent)
      const marginBg = 'repeating-linear-gradient(-45deg,rgba(239,68,68,0.04),rgba(239,68,68,0.04) 3px,transparent 3px,transparent 7px)';
      const marginOverlay = 'rgba(239,68,68,0.06)';
      const borderInner = '1px solid rgba(239,68,68,0.15)';
      // Marge haute
      html += `<div style="position:absolute;left:0;top:0;width:${this.canvasWidth}px;height:${this.safeMargeHautPx}px;background:${marginBg},${marginOverlay};pointer-events:none;border-bottom:${borderInner};"></div>`;
      // Marge basse
      html += `<div style="position:absolute;left:0;bottom:0;width:${this.canvasWidth}px;height:${this.safeMargeBasPx}px;background:${marginBg},${marginOverlay};pointer-events:none;border-top:${borderInner};"></div>`;
      // Marge gauche
      html += `<div style="position:absolute;left:0;top:${this.safeMargeHautPx}px;width:${this.safeMargeGauchePx}px;height:${contentH}px;background:${marginBg},${marginOverlay};pointer-events:none;border-right:${borderInner};"></div>`;
      // Marge droite
      html += `<div style="position:absolute;right:0;top:${this.safeMargeHautPx}px;width:${this.safeMargeDroitePx}px;height:${contentH}px;background:${marginBg},${marginOverlay};pointer-events:none;border-left:${borderInner};"></div>`;

      // Zone de contenu utilisable (bordure délimitant la zone autorisée)
      html += `<div style="position:absolute;left:${this.safeMargeGauchePx}px;top:${this.safeMargeHautPx}px;width:${contentW}px;height:${contentH}px;border:1px dashed rgba(109,94,252,0.25);pointer-events:none;"></div>`;
      for (const block of page.blocks) {
        if(block.visible === false) continue;
        const left = block.x || 0;
        const top = block.y || 0;
        html += `<div style="position:absolute;left:${left}px;top:${top}px;">`;
        html += this.renderBlockWithData(block, mergedData);
        html += `</div>`;
      }
      html += '</div>';
      return { nom: page.nom, html: this.sanitizer.bypassSecurityTrustHtml(html) };
    });
    this.previewPageCount = this.safePreviewPages.length;
    this.currentPreviewPageIndex = Math.min(
      this.currentPreviewPageIndex,
      Math.max(0, this.safePreviewPages.length - 1)
    );
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (
      changes['pages'] ||
      changes['modePagination'] ||
      changes['templateId'] ||
      changes['canvasWidth'] ||
      changes['canvasHeight'] ||
      changes['margeHautPx'] ||
      changes['margeBasPx'] ||
      changes['margeGauchePx'] ||
      changes['margeDroitePx']
    ) {
      this.refreshPreview();
    }
  }

  private refreshPreview(): void {
    this.previewSubscription?.unsubscribe();
    if (this.previewRequestTimer) clearTimeout(this.previewRequestTimer);
    this.previewHtml = null;
    this.renderPreview();
  }

  // --- Zoom ---
  zoomIn(): void { this.fitToWidth = false; this.zoom = Math.min(this.zoom + this.step, this.maxZoom); }
  zoomOut(): void { this.fitToWidth = false; this.zoom = Math.max(this.zoom - this.step, this.minZoom); }
  resetZoom(): void { this.fitToWidth = true; this.updateFitZoom(); }

  private updateFitZoom(): void {
    if (!this.fitToWidth || !this.previewViewport) return;
    const availableWidth = this.previewViewport.nativeElement.clientWidth - 40;
    this.zoom = Math.max(this.minZoom, Math.min(1, availableWidth / this.canvasWidth));
  }

  // --- Dimensions par défaut ---
  private defaultDimensions(type: string): { w: number; h: number } {
    const map: Record<string, {w:number, h:number}> = {
      titre: {w:400, h:40},
      texte: {w:400, h:40},
      tableau: {w:750, h:150},
      ligne: {w:780, h:10},
      image: {w:150, h:150},
      rectangle: {w:150, h:100},
      cercle: {w:100, h:100},
      qrcode: {w:100, h:100},
      codebarre: {w:160, h:60},
      signature: {w:180, h:70},
      graphique: {w:300, h:180},
    };
    return map[type] || {w:200, h:50};
  }

  // --- Wrapper qui impose les dimensions ---
  private wrapBlock(block: DesignBlock, innerHtml: string): string {
    const fallback = this.defaultDimensions(block.type);
    const w = block.largeurBox ?? fallback.w;
    const h = block.hauteurBox ?? fallback.h;
    const isTable = block.type === 'tableau';
    const isDynamicTable = isTable && !block.lignes;
    const isSliced = isTable && !!(block as any)._tableSlice;
    const parts: string[] = [];
    if (block.rotation) parts.push(`transform:rotate(${block.rotation}deg);transform-origin:center center;`);
    if (block.opacite !== undefined && block.opacite !== 100) parts.push(`opacity:${block.opacite / 100};`);
    // Tableaux dynamiques ou slicés : hauteur automatique, pas de débordement masqué
    if (isDynamicTable || isSliced) {
      return `<div style="width:${w}px;height:auto;overflow:visible;box-sizing:border-box;position:relative;${parts.join('')}">${innerHtml}</div>`;
    }
    return `<div style="width:${w}px;height:${h}px;overflow:hidden;box-sizing:border-box;position:relative;${parts.join('')}">${innerHtml}</div>`;
  }

  // --- Rendu de chaque bloc avec son wrapper ---
  private renderBlockWithData(block: DesignBlock, mockData: Record<string, any>): string {
    let innerHtml = '';
    switch (block.type) {
      case 'titre':
      case 'texte': {
        const tag = block.type === 'titre' ? 'h1' : 'p';
        const style = `font-size:${block.style?.fontSize || 12}px; font-weight:${block.style?.bold ? 'bold' : 'normal'}; font-style:${block.style?.italic ? 'italic' : 'normal'}; text-decoration:${block.style?.underline ? 'underline' : 'none'}; text-align:${block.style?.align || 'left'}; color:${block.style?.color || '#000000'}; font-family:${block.style?.fontFamily || 'inherit'}; margin:0;`;
        innerHtml = `<${tag} style="${style}">${this.replaceVars(block.contenu || '', mockData)}</${tag}>`;
        break;
      }
      case 'tableau':
        innerHtml = this.renderTableWithData(block, mockData);
        break;
      case 'ligne': {
        const epaisseur = block.style?.epaisseur || 1;
        const couleur = block.style?.couleur || '#000';
        const largeur = block.style?.largeur || 100;
        innerHtml = `<div style="height:${epaisseur}px; background-color:${couleur}; width:${largeur}%;"></div>`;
        break;
      }
      case 'image': {
        const url = this.replaceVars(block.url || '', mockData);
        const largeur = block.style?.largeur || 100;
        const align = block.style?.align || 'left';
        let imgStyle = `width:${largeur}px;`;
        if (align === 'center') imgStyle += 'display:block;margin:0 auto;';
        else if (align === 'right') imgStyle += 'display:block;margin-left:auto;';
        innerHtml = `<img src="${this.escape(url)}" style="${imgStyle}" alt="aperçu" />`;
        break;
      }
      case 'rectangle':
      case 'cercle': {
        const fill = block.style?.fill || '#e5e7eb';
        const couleur = block.style?.couleur || '#94a3b8';
        const epaisseur = block.style?.epaisseur ?? 1;
        const radius = block.type === 'cercle' ? '50%' : `${block.style?.borderRadius ?? 0}px`;
        // On utilise 100% pour remplir le wrapper
        innerHtml = `<div style="width:100%; height:100%; min-width:10px; min-height:10px; background:${fill}; border:${epaisseur}px solid ${couleur}; border-radius:${radius}; box-sizing:border-box;"></div>`;
        break;
      }
      case 'qrcode':
      case 'codebarre': {
        const label = block.type === 'qrcode' ? 'QR Code' : 'Code-barres';
        innerHtml = `<div style="width:100%; height:100%; border:2px dashed #d1d5db; background:#f9fafb; display:flex; align-items:center; justify-content:center; color:#6b7280; font-size:10px; font-family:Arial,sans-serif; box-sizing:border-box;">${label} (généré au PDF)</div>`;
        break;
      }
      case 'signature': {
        innerHtml = `<div style="width:100%; height:100%; border-bottom:1px solid #333; display:flex; align-items:flex-end; justify-content:center; padding-bottom:4px; font-family:cursive; color:#999; font-size:12px; box-sizing:border-box;">Signature</div>`;
        break;
      }
      case 'graphique': {
        innerHtml = this.renderGraphiqueWithData(block, mockData);
        break;
      }
      default:
        return '';
    }
    // Tous les blocs sont wrappés avec leurs dimensions
    return this.wrapBlock(block, innerHtml);
  }

  // --- Pagination intelligente des tableaux ---

  /** Informations sur les dimensions d'un tableau pour la pagination */
  private getTableRowInfo(block: DesignBlock, mockData: Record<string, any>): {
    totalRows: number; headerHeight: number; rowHeight: number; totalHeight: number;
  } {
    if (block.lignes) {
      // La première ligne est l'en-tête, le reste sont des données
      const dataRowCount = Math.max(0, block.lignes.length - 1);
      // Hauteur réelle d'une ligne CSS : font-size:14px ≈ 17px + padding 3+3px + border 1px ≈ 24px
      // On utilise 25px pour inclure une marge de sécurité d'1px
      const headerHeight = 25;
      const rowHeight = 25;
      return { totalRows: dataRowCount, headerHeight, rowHeight, totalHeight: headerHeight + dataRowCount * rowHeight };
    }
    const source = (block.source || '').replace('{{', '').replace('}}', '').trim();
    const rows: any[] = mockData[source] || [];
    const totalRows = rows.length;
    const headerHeight = 25;
    const rowHeight = 25;
    return { totalRows, headerHeight, rowHeight, totalHeight: headerHeight + totalRows * rowHeight };
  }

  /** Logique de pagination pour un bloc tableau */
  private paginateTableBlock(
    block: DesignBlock, top: number, contentH: number,
    pageFragments: { block: DesignBlock; pageOffset: number; tableSlice?: { startRow: number; endRow: number } }[],
    mockData: Record<string, any>
  ): void {
    const tableInfo = this.getTableRowInfo(block, mockData);
    const availableTop = Math.max(0, contentH - (top - this.safeMargeHautPx));

    if (availableTop <= 0 || tableInfo.totalRows === 0) {
      // Pas de place ou tableau vide — sur la page suivante entière
      pageFragments.push({ block: { ...block, y: this.safeMargeHautPx }, pageOffset: 1 });
      return;
    }

    if (tableInfo.totalHeight <= availableTop) {
      // Tout le tableau tient sur la page actuelle
      pageFragments.push({ block: { ...block, y: top }, pageOffset: 0 });
      return;
    }

    // Le tableau déborde — calculer combien de lignes tiennent
    const headerH = tableInfo.headerHeight;
    const rowH = tableInfo.rowHeight;
    const rowsThatFit = Math.floor((availableTop - headerH) / rowH);

    if (rowsThatFit <= 0) {
      // Pas de place même pour 1 ligne de données — page suivante
      this.splitTableContinuation(block, tableInfo, 0, pageFragments);
      return;
    }

    // Première partie : lignes qui tiennent sur la page actuelle
    pageFragments.push({
      block: { ...block, y: top },
      pageOffset: 0,
      tableSlice: { startRow: 0, endRow: rowsThatFit }
    });

    // Suite sur les pages suivantes
    this.splitTableContinuation(block, tableInfo, rowsThatFit, pageFragments);
  }

  /** Fractionne la suite d'un tableau en fragments à partir de startRow */
  private splitTableContinuation(
    block: DesignBlock,
    tableInfo: { totalRows: number; headerHeight: number; rowHeight: number },
    startRow: number,
    pageFragments: { block: DesignBlock; pageOffset: number; tableSlice?: { startRow: number; endRow: number } }[]
  ): void {
    const contentH = this.canvasHeight - this.safeMargeHautPx - this.safeMargeBasPx;
    let currentRow = startRow;
    let currentPageOffset = pageFragments.length > 0
      ? Math.max(0, ...pageFragments.map(f => f.pageOffset)) + 1
      : 1;

    while (currentRow < tableInfo.totalRows) {
      const rowsThatFit = Math.max(1, Math.floor((contentH - tableInfo.headerHeight) / tableInfo.rowHeight));
      const endRow = Math.min(currentRow + rowsThatFit, tableInfo.totalRows);

      pageFragments.push({
        block: { ...block, y: this.safeMargeHautPx },
        pageOffset: currentPageOffset,
        tableSlice: { startRow: currentRow, endRow }
      });

      currentRow = endRow;
      currentPageOffset++;
    }
  }

  /** Construit un bloc tableau ne contenant qu'un sous-ensemble de lignes */
  private buildSlicedTableBlock(block: DesignBlock, slice: { startRow: number; endRow: number }): DesignBlock {
    const sliced = { ...block };
    // Stocker le slice pour renderTableWithData
    (sliced as any)._tableSlice = slice;
    return sliced;
  }

  // --- Tableau ---
  private renderTableWithData(block: DesignBlock, mockData: Record<string, any>): string {
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
        table += `<td${colspan}${rowspan} style="border:1px solid ${bordure};padding:3px 5px;min-width:90px;font-weight:600;${bg}${color}">${this.replaceVars(cell.value || '', mockData)}</td>`;
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
          table += `<td${colspan}${rowspan} style="border:1px solid ${bordure};padding:3px 5px;min-width:90px;${bg}${color}">${this.replaceVars(cell.value || '', mockData)}</td>`;
        }
        table += '</tr>';
      }
      return table + '</table>';
    }

    // Tableau dynamique
    const source = block.source || '';
    const sourceClean = source.replace('{{', '').replace('}}', '').trim();
    const allRows: any[] = mockData[sourceClean] || [];

    const tableStyle = `border-collapse:collapse;width:100%;font-family:Arial, sans-serif;font-size:14px;color:${texteDefaut};font-weight:400;`;
    const cellStyle = `border:1px solid ${bordure};padding:3px 5px;min-width:90px;text-align:left;`;
    const headerStyle = cellStyle + 'background:#f5f5f5;font-weight:600;';

    let table = `<table style="${tableStyle}">`;
    // En-tête toujours présent
    table += '<tr>';
    for (const col of block.colonnes || []) {
      table += `<td style="${headerStyle}">${this.escape(col.titre || col.variable)}</td>`;
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
        table += `<td style="${cellStyle}">${this.escape(String(val))}</td>`;
      }
      table += '</tr>';
    }
    return table + '</table>';
  }

  // --- Graphique ---
  private renderGraphiqueWithData(block: DesignBlock, mockData: Record<string, any>): string {
    const source = (block.source || '').replace('{{', '').replace('}}', '').trim();
    const items = mockData[source];
    const w = block.largeurBox || 300;
    const h = block.hauteurBox || 180;
    if (!Array.isArray(items) || items.length === 0) {
      return `<div style="width:100%; height:100%; border:1px dashed #ccc; display:flex; align-items:center; justify-content:center; color:#999; font-size:11px; font-family:Arial,sans-serif; box-sizing:border-box;">Graphique (${source})</div>`;
    }
    const max = Math.max(...items.map((i: any) => Number(i.value) || 0), 1);
    const barAreaHeight = h - 40; // on connaît h
    let bars = '';
    for (const item of items) {
      const value = Number(item.value) || 0;
      const barHeight = Math.max(2, (value / max) * barAreaHeight);
      bars += `<div style="display:flex;flex-direction:column;align-items:center;flex:1;">
        <div style="width:100%; background:#6d5efc; border-radius:3px 3px 0 0; height:${barHeight}px;"></div>
        <span style="font-size:9px; color:#555; margin-top:4px; text-align:center;">${this.escape(String(item.label))}</span>
      </div>`;
    }
    return `<div style="width:100%; height:100%; display:flex; align-items:flex-end; gap:6px; border-left:1px solid #ccc; border-bottom:1px solid #ccc; padding:8px; box-sizing:border-box; font-family:Arial,sans-serif;">${bars}</div>`;
  }

  // --- Utilitaires ---
  private replaceVars(text: string, mockData: Record<string, any>): string {
    if (!text) return '';
    return text.replace(/\{\{(.+?)\}\}/g, (match, varName) => {
      const trimmed = varName.trim();
      return mockData[trimmed] !== undefined ? String(mockData[trimmed]) : match;
    });
  }

  private escape(input: string): string {
    if (!input) return '';
    return input.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  private loadAutoPreview(): void {
    this.previewSubscription?.unsubscribe();
    this.previewPageCount = 0;
    this.previewFrameHeight = 800;

    if (!this.templateId) {
      this.previewHtml = null;
      return;
    }

    const allBlocks = this.pages.flatMap(p => p.blocks);
    const mockData = this.mockDataService.generate(allBlocks);
    const realValues = this.fillerdata.getValues();
    const mergedData = { ...mockData, ...realValues };

    this.previewSubscription = this.api.getPreviewHtml(this.templateId, mergedData).subscribe({
      next: (html) => {
        this.previewHtml = html;
        this.safePreviewHtmlValue = this.sanitizer.bypassSecurityTrustHtml(html);
      },
      error: (err) => {
        console.error('Erreur preview auto', err);
        this.previewHtml = null;
        this.safePreviewHtmlValue = this.sanitizer.bypassSecurityTrustHtml('');
      }
    });
  }

  onAutoPreviewLoad(event: Event): void {
    const frame = event.target as HTMLIFrameElement;
    const document = frame.contentDocument;
    if (!document) return;

    const pages = document.querySelectorAll('.page').length;
    this.previewPageCount = pages;
    this.previewFrameHeight = Math.max(
      800,
      document.documentElement.scrollHeight,
      document.body?.scrollHeight || 0
    );
  }

  get safePreviewHtml() {
    return this.safePreviewHtmlValue || '';
  }

}