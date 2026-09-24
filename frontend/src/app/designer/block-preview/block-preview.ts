import { AfterViewInit, ChangeDetectorRef, Component, ElementRef, Input, OnChanges, OnDestroy, OnInit, SimpleChanges, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DesignBlock, DesignPage } from '../models/design-block.model';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { MockDataService } from '../services/mock-data.service';
import { LucideAngularModule, Sparkles, Plus, Minus, RotateCcw, ArrowLeft, ArrowRight } from 'lucide-angular';
import { FillerDataService } from '../services/filler-data';
import { TemplateApiService } from '../../services/template-api';
import { Subscription } from 'rxjs';
import {
  BlockHtmlRenderer,
  RenderContext,
  TextBlockRenderer,
  TableBlockRenderer,
  ChartBlockRenderer,
  ShapeBlockRenderer,
  BarcodeBlockRenderer
} from './renderers/index';

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
  @Input() couleurFond: string = '#ffffff';

  @Input() headerActif: boolean = false;
  @Input() hauteurHeaderMm: number = 15;
  @Input() headerContenu?: string | null = null;
  @Input() headerAlignement: string = 'GAUCHE';
  @Input() headerAfficherSurPremierePage: boolean = true;
  @Input() headerLigneSeparation: boolean = true;
  @Input() headerCouleurLigne: string = '#cccccc';

  @Input() footerActif: boolean = false;
  @Input() hauteurFooterMm: number = 12;
  @Input() footerContenu?: string | null = null;
  @Input() footerAlignement: string = 'CENTRE';
  @Input() footerAfficherSurPremierePage: boolean = true;
  @Input() footerLigneSeparation: boolean = true;
  @Input() footerCouleurLigne: string = '#cccccc';
  @Input() numerotationPage: boolean = true;
  @Input() formatNumerotation: string = 'PAGE_X_SUR_Y';

  private readonly defaultMarginPx = Math.round(10 * 96 / 25.4); // ~38px

  get safeMargeHautPx(): number { return (this.margeHautPx != null && this.margeHautPx > 0) ? this.margeHautPx : this.defaultMarginPx; }
  get safeMargeBasPx(): number { return (this.margeBasPx != null && this.margeBasPx > 0) ? this.margeBasPx : this.defaultMarginPx; }
  get safeMargeGauchePx(): number { return (this.margeGauchePx != null && this.margeGauchePx > 0) ? this.margeGauchePx : this.defaultMarginPx; }
  get safeMargeDroitePx(): number { return (this.margeDroitePx != null && this.margeDroitePx > 0) ? this.margeDroitePx : this.defaultMarginPx; }

  get headerHeightPx(): number {
    return this.headerActif ? Math.round((this.hauteurHeaderMm || 15) * 96 / 25.4) : 0;
  }

  get footerHeightPx(): number {
    return this.footerActif ? Math.round((this.hauteurFooterMm || 12) * 96 / 25.4) : 0;
  }

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

  private readonly renderers: BlockHtmlRenderer[] = [
    new TextBlockRenderer(),
    new TableBlockRenderer(),
    new ChartBlockRenderer(),
    new ShapeBlockRenderer(),
    new BarcodeBlockRenderer(),
  ];

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
    queueMicrotask(() => {
      this.updateFitZoom();
      this.cdr.detectChanges();
    });
    this.resizeObserver = new ResizeObserver(() => {
      this.updateFitZoom();
      this.cdr.detectChanges();
    });
    if (this.previewViewport) {
      this.resizeObserver.observe(this.previewViewport.nativeElement);
    }
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

    const effectiveTopPx = this.safeMargeHautPx + this.headerHeightPx;
    const effectiveBottomPx = this.safeMargeBasPx + this.footerHeightPx;
    const contentH = this.canvasHeight - effectiveTopPx - effectiveBottomPx;

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
            : Math.max(effectiveTopPx, top - pageOffset * this.canvasHeight);
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

    const totalPages = generatedPages.length;
    const today = new Date().toLocaleDateString('fr-FR');

    this.safePreviewPages = generatedPages.map((page, pageIdx) => {
      const pageNum = pageIdx + 1;
      const contentW = this.canvasWidth - this.safeMargeGauchePx - this.safeMargeDroitePx;
      let html = `<div style="position:relative;width:${this.canvasWidth}px;height:${this.canvasHeight}px;background:${this.couleurFond || 'white'};overflow:hidden;margin:0 auto;">`;

      // Zones de marges interdites (hachures + fond semi-transparent)
      const marginBg = 'repeating-linear-gradient(-45deg,rgba(239,68,68,0.04),rgba(239,68,68,0.04) 3px,transparent 3px,transparent 7px)';
      const marginOverlay = 'rgba(239,68,68,0.06)';
      const borderInner = '1px solid rgba(239,68,68,0.15)';
      // Marge haute
      html += `<div style="position:absolute;left:0;top:0;width:${this.canvasWidth}px;height:${this.safeMargeHautPx}px;background:${marginBg},${marginOverlay};pointer-events:none;border-bottom:${borderInner};"></div>`;
      // Marge basse
      html += `<div style="position:absolute;left:0;bottom:0;width:${this.canvasWidth}px;height:${this.safeMargeBasPx}px;background:${marginBg},${marginOverlay};pointer-events:none;border-top:${borderInner};"></div>`;
      // Marge gauche
      html += `<div style="position:absolute;left:0;top:${this.safeMargeHautPx}px;width:${this.safeMargeGauchePx}px;height:${this.canvasHeight - this.safeMargeHautPx - this.safeMargeBasPx}px;background:${marginBg},${marginOverlay};pointer-events:none;border-right:${borderInner};"></div>`;
      // Marge droite
      html += `<div style="position:absolute;right:0;top:${this.safeMargeHautPx}px;width:${this.safeMargeDroitePx}px;height:${this.canvasHeight - this.safeMargeHautPx - this.safeMargeBasPx}px;background:${marginBg},${marginOverlay};pointer-events:none;border-left:${borderInner};"></div>`;

      // En-tête (si actif)
      const showHeader = this.headerActif && (pageNum > 1 || this.headerAfficherSurPremierePage !== false);
      if (showHeader) {
        let hText = this.headerContenu || '';
        hText = hText.replace(/\{page\}/g, String(pageNum)).replace(/\{pages\}/g, String(totalPages)).replace(/\{date\}/g, today);
        const textAlign = (this.headerAlignement === 'DROITE' || this.headerAlignement === 'RIGHT') ? 'right' : (this.headerAlignement === 'CENTRE' || this.headerAlignement === 'CENTER' ? 'center' : 'left');
        const borderBottom = this.headerLigneSeparation ? `border-bottom:1px solid ${this.headerCouleurLigne || '#cccccc'};` : '';
        html += `<div style="position:absolute;left:${this.safeMargeGauchePx}px;top:${this.safeMargeHautPx}px;width:${contentW}px;height:${this.headerHeightPx}px;overflow:hidden;box-sizing:border-box;display:flex;align-items:center;justify-content:${textAlign === 'right' ? 'flex-end' : (textAlign === 'center' ? 'center' : 'flex-start')};font-family:Arial,sans-serif;font-size:12px;color:#555;padding:0 6px;${borderBottom}">${hText}</div>`;
      }

      // Pied de page (si actif)
      const showFooter = this.footerActif && (pageNum > 1 || this.footerAfficherSurPremierePage !== false);
      if (showFooter) {
        let fText = this.footerContenu || (this.numerotationPage ? (this.formatNumerotation === 'PAGE_X' ? 'Page {page}' : 'Page {page} / {pages}') : '');
        fText = fText.replace(/\{page\}/g, String(pageNum)).replace(/\{pages\}/g, String(totalPages)).replace(/\{date\}/g, today);
        const textAlign = (this.footerAlignement === 'DROITE' || this.footerAlignement === 'RIGHT') ? 'right' : (this.footerAlignement === 'CENTRE' || this.footerAlignement === 'CENTER' ? 'center' : 'left');
        const borderTop = this.footerLigneSeparation ? `border-top:1px solid ${this.footerCouleurLigne || '#cccccc'};` : '';
        const footerTop = this.canvasHeight - this.safeMargeBasPx - this.footerHeightPx;
        html += `<div style="position:absolute;left:${this.safeMargeGauchePx}px;top:${footerTop}px;width:${contentW}px;height:${this.footerHeightPx}px;overflow:hidden;box-sizing:border-box;display:flex;align-items:center;justify-content:${textAlign === 'right' ? 'flex-end' : (textAlign === 'center' ? 'center' : 'flex-start')};font-family:Arial,sans-serif;font-size:12px;color:#555;padding:0 6px;${borderTop}">${fText}</div>`;
      }

      // Zone de contenu utilisable (bordure délimitant la zone autorisée)
      html += `<div style="position:absolute;left:${this.safeMargeGauchePx}px;top:${effectiveTopPx}px;width:${contentW}px;height:${contentH}px;border:1px dashed rgba(109,94,252,0.25);pointer-events:none;"></div>`;
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
    if (availableWidth > 0 && this.canvasWidth > 0) {
      this.zoom = Math.max(this.minZoom, Math.min(1, availableWidth / this.canvasWidth));
    }
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
    const context: RenderContext = {
      mockData,
      replaceVars: (text, data) => this.replaceVars(text, data),
      escape: (text) => this.escape(text),
    };

    const renderer = this.renderers.find(r => r.supports(block.type));
    if (!renderer) {
      return '';
    }

    const innerHtml = renderer.render(block, context);
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

  // Note: Table and Chart HTML rendering is delegated to TableBlockRenderer and ChartBlockRenderer


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