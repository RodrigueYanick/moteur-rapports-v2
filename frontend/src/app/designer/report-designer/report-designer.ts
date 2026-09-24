import { Component, EventEmitter, Input, Output, OnInit, SimpleChanges, HostListener, ChangeDetectorRef } from '@angular/core';
import { DesignBlock, DesignPage } from '../models/design-block.model';
import { CommonModule } from '@angular/common';
import { BlockEditor } from "../block-editor/block-editor";
import { BlockPreview } from '../block-preview/block-preview';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { DesignCanvas } from '../design-canvas/design-canvas';
import { Sidebar } from '../sidebar/sidebar';
import { FormsModule } from '@angular/forms';
import {
  LucideAngularModule, Undo2, Redo2, Grid3x3, Magnet, Ruler, Minus, Plus, Eye,
  Download, Rocket, Share2, Save, FileText, Check, Loader2, Ruler as RulerIcon,
  Edit, X, Lock, Copy, Settings, Trash2, AlignLeft, AlignCenter, AlignRight,
  ArrowUp, ArrowDown, Layers, MoveHorizontal, MoveVertical
} from 'lucide-angular';
import { Variable } from '../../models/variable.model';
import { TemplateFiller } from "../template-filler/template-filler";
import { FillerDataService } from '../services/filler-data';
import { MockDataService } from '../services/mock-data.service';
import { TemplateApiService } from '../../services/template-api';
import { DesignerClipboardService } from '../services/designer-clipboard.service';
import { DesignerSelectionService } from '../services/designer-selection.service';
import { DesignerGeometryService, UsableArea } from '../services/designer-geometry.service';
import { DesignerHistoryService } from '../services/designer-history.service';
import { DesignerStore } from '../services/designer.store';
import { OnboardingService } from '../../shared/services/onboarding.service';
import { OnboardingTourComponent } from '../../shared/components/onboarding-tour/onboarding-tour.component';
import { DesignerToolbarComponent } from '../components/designer-toolbar/designer-toolbar.component';
import { DesignerPageTabsComponent } from '../components/designer-page-tabs/designer-page-tabs.component';

@Component({
  selector: 'app-report-designer',
  standalone: true,
  imports: [
    CommonModule, BlockEditor, BlockPreview, DragDropModule,
    DesignCanvas, Sidebar, FormsModule, LucideAngularModule, TemplateFiller,
    OnboardingTourComponent, DesignerToolbarComponent, DesignerPageTabsComponent
  ],
  templateUrl: './report-designer.html',
  styleUrl: './report-designer.scss',
})
export class ReportDesigner implements OnInit {
  @Input() pages: DesignPage[] = [];
  @Input() templateName = '';
  @Input() savingStatus: 'idle' | 'saving' | 'saved' = 'idle';
  @Input() templateId: string | null = null;
  @Input() templateStatus: 'BROUILLON' | 'PUBLIE' | 'ARCHIVE' = 'BROUILLON';
  @Input() formatPapier: string = 'A4';
  @Input() largeurMm?: number | null;
  @Input() hauteurMm?: number | null;
  @Input() modePagination: 'FIXED' | 'AUTO' = 'FIXED';
  @Input() margeHautMm: number = 10;
  @Input() margeBasMm: number = 10;
  @Input() margeGaucheMm: number = 10;
  @Input() margeDroiteMm: number = 10;
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

  @Output() pagesChange = new EventEmitter<DesignPage[]>();
  @Output() pageSettingsChange = new EventEmitter<{
    formatPapier?: string;
    modePagination?: 'FIXED' | 'AUTO';
    margeHautMm?: number;
    margeBasMm?: number;
    margeGaucheMm?: number;
    margeDroiteMm?: number;
  }>();
  @Output() publishRequest = new EventEmitter<void>();
  @Output() exportRequest = new EventEmitter<Record<string, any> | undefined>();
  @Output() previewRequest = new EventEmitter<void>();
  @Output() manualSaveRequest = new EventEmitter<void>();
  @Output() duplicateRequest = new EventEmitter<void>();

  selectedBlock: DesignBlock | null = null;
  explicitVariables: Variable[] = [];
  activePageIndex = 0;
  editingPageIndex: number | null = null;
  showPageSettingsModal = false;

  editingSettings = {
    formatPapier: 'A4',
    modePagination: 'FIXED' as 'FIXED' | 'AUTO',
    margeHautMm: 10,
    margeBasMm: 10,
    margeGaucheMm: 10,
    margeDroiteMm: 10
  };

  private isUndoRedoAction = false;
  private ignoreNextChange = false;
  private variableCounters: Record<string, number> = {};

  readonly icons = {
    undo: Undo2, redo: Redo2, grid: Grid3x3, snap: Magnet, guides: Ruler,
    zoomOut: Minus, zoomIn: Plus, preview: Eye, export: Download, publish: Rocket,
    share: Share2, save: Save, file: FileText, check: Check, loader: Loader2,
    Ruler: RulerIcon, edit: Edit, fileText: FileText, close: X, lock: Lock, copy: Copy,
    settings: Settings, trash: Trash2,
    alignLeft: AlignLeft, alignCenter: AlignCenter, alignRight: AlignRight,
    alignTop: ArrowUp, alignBottom: ArrowDown,
    distributeH: MoveHorizontal, distributeV: MoveVertical,
    layers: Layers
  };

  constructor(
    private api: TemplateApiService,
    private fillerData: FillerDataService,
    private mockDataService: MockDataService,
    private clipboardService: DesignerClipboardService,
    private selectionService: DesignerSelectionService,
    private geometryService: DesignerGeometryService,
    private historyService: DesignerHistoryService,
    public designerStore: DesignerStore,
    private onboardingService: OnboardingService,
    private cdr: ChangeDetectorRef
  ) {}

  // ---------- Getters UI délégués au Store ----------
  get showGrid(): boolean { return this.designerStore.showGrid(); }
  get snapEnabled(): boolean { return this.designerStore.snapEnabled(); }
  get showGuides(): boolean { return this.designerStore.showGuides(); }
  get zoomPercent(): number { return this.designerStore.zoomPercent(); }
  get fillingMode(): boolean { return this.designerStore.fillingMode(); }
  get lockedMessage(): string | null { return this.designerStore.lockedMessage(); }
  get isLocked(): boolean { return this.templateStatus !== 'BROUILLON'; }
  get isAutoPagination(): boolean { return this.modePagination === 'AUTO'; }

  toggleGrid(): void { this.designerStore.toggleGrid(); }
  toggleSnap(): void { this.designerStore.toggleSnap(); }
  toggleGuides(): void { this.designerStore.toggleGuides(); }
  toggleFillingMode(): void { this.designerStore.toggleFillingMode(); }
  zoomIn(): void { this.designerStore.zoomIn(); }
  zoomOut(): void { this.designerStore.zoomOut(); }
  resetZoom(): void { this.designerStore.resetZoom(); }
  showLockedMessage(msg?: string): void { this.designerStore.showLockedMessage(msg); }

  // ---------- Accès aux blocs & pages ----------
  get blocks(): DesignBlock[] {
    return this.pages[this.activePageIndex]?.blocks || [];
  }

  set blocks(value: DesignBlock[]) {
    if (this.pages[this.activePageIndex]) {
      this.pages[this.activePageIndex].blocks = value;
    }
  }

  get allBlocksFlat(): DesignBlock[] {
    return this.pages.flatMap(p => p.blocks);
  }

  get virtualCanvasHeight(): number {
    if (!this.isAutoPagination) return this.canvasHeight;
    let maxY = 0;
    for (const block of this.blocks) {
      const bottom = (block.y || 0) + (block.hauteurBox || 100);
      if (bottom > maxY) maxY = bottom;
    }
    return Math.max(this.canvasHeight, maxY + 200);
  }

  // ---------- Dimensions & Marges déléguées au GeometryService ----------
  get canvasWidth(): number {
    return this.geometryService.getPaperDimensions(this.formatPapier, this.largeurMm, this.hauteurMm).width;
  }

  get canvasHeight(): number {
    return this.geometryService.getPaperDimensions(this.formatPapier, this.largeurMm, this.hauteurMm).height;
  }

  get effectiveMargeHautMm(): number { return (this.margeHautMm != null && this.margeHautMm > 0) ? this.margeHautMm : 10; }
  get effectiveMargeBasMm(): number { return (this.margeBasMm != null && this.margeBasMm > 0) ? this.margeBasMm : 10; }
  get effectiveMargeGaucheMm(): number { return (this.margeGaucheMm != null && this.margeGaucheMm > 0) ? this.margeGaucheMm : 10; }
  get effectiveMargeDroiteMm(): number { return (this.margeDroiteMm != null && this.margeDroiteMm > 0) ? this.margeDroiteMm : 10; }

  get paperFormat(): string { return this.formatPapier; }
  get margeHautPx(): number { return this.usableAreaPx.minY; }
  get margeBasPx(): number { return this.canvasHeight - this.usableAreaPx.maxY; }
  get margeGauchePx(): number { return this.usableAreaPx.minX; }
  get margeDroitePx(): number { return this.canvasWidth - this.usableAreaPx.maxX; }

  onCanvasPreviewChange(blocks: DesignBlock[]): void {
    // Aperçu dynamique
  }

  get usableAreaPx(): UsableArea {
    return this.geometryService.getUsableArea(
      this.canvasWidth, this.canvasHeight,
      this.margeGaucheMm, this.margeDroiteMm, this.margeHautMm, this.margeBasMm
    );
  }

  private clampBlockToUsableArea(block: DesignBlock): void {
    this.geometryService.clampBlockToUsableArea(block, this.usableAreaPx);
  }

  // ---------- Lifecycle Hooks ----------
  ngOnInit(): void {
    if (!this.pages || this.pages.length === 0) {
      this.pages = [{ id: crypto.randomUUID(), nom: 'Page 1', blocks: [] }];
    }
    this.historyService.initialize(this.pages);
    if (this.templateId) {
      this.loadVariables(this.templateId);
    }
    setTimeout(() => this.onboardingService.startTour(), 150);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['templateId'] && this.templateId) {
      this.loadVariables(this.templateId);
    }
    if (changes['pages'] && !changes['pages'].firstChange) {
      if (this.ignoreNextChange) {
        this.ignoreNextChange = false;
        return;
      }
      this.activePageIndex = 0;
      this.historyService.initialize(this.pages);
    }
  }

  private loadVariables(templateId: string): void {
    this.api.getVariables(templateId).subscribe({
      next: (vars) => {
        queueMicrotask(() => {
          this.explicitVariables = vars;
          this.cdr.markForCheck();
        });
      },
      error: (err) => console.error('Erreur chargement variables pour autocomplétion', err)
    });
  }

  // ---------- Historique délégué au HistoryService ----------
  get canUndo(): boolean { return this.historyService.canUndo; }
  get canRedo(): boolean { return this.historyService.canRedo; }

  undo(): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
    const prev = this.historyService.undo(this.pages);
    if (prev) {
      this.isUndoRedoAction = true;
      this.pages = prev;
      if (this.activePageIndex >= this.pages.length) this.activePageIndex = this.pages.length - 1;
      this.emitPagesChange();
      this.isUndoRedoAction = false;
    }
  }

  redo(): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
    const next = this.historyService.redo(this.pages);
    if (next) {
      this.isUndoRedoAction = true;
      this.pages = next;
      if (this.activePageIndex >= this.pages.length) this.activePageIndex = this.pages.length - 1;
      this.emitPagesChange();
      this.isUndoRedoAction = false;
    }
  }

  private afterPagesChanged(): void {
    if (!this.isUndoRedoAction) {
      this.historyService.pushState(this.pages);
    }
    this.emitPagesChange();
  }

  private emitPagesChange(): void {
    this.ignoreNextChange = true;
    this.pagesChange.emit([...this.pages]);
  }

  // ---------- Gestion des Pages ----------
  addPage(): void {
    if (this.isLocked || this.isAutoPagination) { this.showLockedMessage(); return; }
    const newPage: DesignPage = { id: crypto.randomUUID(), nom: `Page ${this.pages.length + 1}`, blocks: [] };
    this.pages = [...this.pages, newPage];
    this.activePageIndex = this.pages.length - 1;
    this.selectedBlock = null;
    this.afterPagesChanged();
  }

  removePage(index: number, event: Event): void {
    event.stopPropagation();
    if (this.isLocked || this.isAutoPagination) { this.showLockedMessage(); return; }
    if (this.pages.length <= 1) return;
    this.pages = this.pages.filter((_, i) => i !== index);
    if (this.activePageIndex >= this.pages.length) this.activePageIndex = this.pages.length - 1;
    else if (this.activePageIndex > index) this.activePageIndex--;
    this.selectedBlock = null;
    this.afterPagesChanged();
  }

  duplicatePage(index: number, event: Event): void {
    event.stopPropagation();
    if (this.isLocked) { this.showLockedMessage(); return; }
    const source = this.pages[index];
    const clonedBlocks: DesignBlock[] = JSON.parse(JSON.stringify(source.blocks))
      .map((b: DesignBlock) => ({ ...b, id: crypto.randomUUID() }));
    clonedBlocks.forEach(b => this.clampBlockToUsableArea(b));
    const copy: DesignPage = { id: crypto.randomUUID(), nom: `${source.nom} (copie)`, blocks: clonedBlocks };
    this.pages = [...this.pages.slice(0, index + 1), copy, ...this.pages.slice(index + 1)];
    this.activePageIndex = index + 1;
    this.afterPagesChanged();
  }

  onPageRemove(evt: { index: number; event: Event }): void {
    this.removePage(evt.index, evt.event);
  }

  onPageDuplicate(evt: { index: number; event: Event }): void {
    this.duplicatePage(evt.index, evt.event);
  }

  switchPage(index: number): void {
    this.activePageIndex = index;
    this.selectedBlock = null;
  }

  startRenamePage(index: number, event: Event): void {
    event.stopPropagation();
    if (this.isLocked) { this.showLockedMessage(); return; }
    this.editingPageIndex = index;
  }

  finishRenamePage(): void {
    this.editingPageIndex = null;
    this.afterPagesChanged();
  }

  // ---------- Paramètres de Page ----------
  openPageSettings(): void {
    this.editingSettings = {
      formatPapier: this.formatPapier || 'A4',
      modePagination: this.modePagination || 'FIXED',
      margeHautMm: this.effectiveMargeHautMm,
      margeBasMm: this.effectiveMargeBasMm,
      margeGaucheMm: this.effectiveMargeGaucheMm,
      margeDroiteMm: this.effectiveMargeDroiteMm,
    };
    this.showPageSettingsModal = true;
  }

  closePageSettings(): void {
    this.showPageSettingsModal = false;
  }

  savePageSettings(): void {
    if (this.isLocked) {
      this.showLockedMessage();
      this.closePageSettings();
      return;
    }
    this.formatPapier = this.editingSettings.formatPapier;
    this.modePagination = this.editingSettings.modePagination;
    this.margeHautMm = Number(this.editingSettings.margeHautMm) || 10;
    this.margeBasMm = Number(this.editingSettings.margeBasMm) || 10;
    this.margeGaucheMm = Number(this.editingSettings.margeGaucheMm) || 10;
    this.margeDroiteMm = Number(this.editingSettings.margeDroiteMm) || 10;

    this.pageSettingsChange.emit({
      formatPapier: this.formatPapier,
      modePagination: this.modePagination,
      margeHautMm: this.margeHautMm,
      margeBasMm: this.margeBasMm,
      margeGaucheMm: this.margeGaucheMm,
      margeDroiteMm: this.margeDroiteMm,
    });
    this.closePageSettings();
  }

  // ---------- Gestion des Blocs ----------
  addBlock(type: DesignBlock['type']): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
    const area = this.usableAreaPx;
    const newBlock: DesignBlock = {
      id: crypto.randomUUID(), type, contenu: '', style: {},
      x: area.minX + 10, y: area.minY + 10, visible: true, locked: false, rotation: 0
    };

    switch (type) {
      case 'titre':
        newBlock.contenu = `{{${this.generateVariableName('titre')}}}`;
        newBlock.style = { fontSize: 18, bold: true, align: 'center' };
        break;
      case 'texte':
        newBlock.contenu = `{{${this.generateVariableName('texte')}}}`;
        newBlock.style = { fontSize: 12 };
        break;
      case 'tableau':
        newBlock.lignes = [[{ value: '' }, { value: '' }], [{ value: '' }, { value: '' }]];
        break;
      case 'ligne':
        newBlock.style = { epaisseur: 1, couleur: '#000000', largeur: 100 };
        break;
      case 'image':
        newBlock.url = `{{${this.generateVariableName('image')}}}`;
        newBlock.style = { largeur: 100, align: 'left' };
        break;
      case 'rectangle':
        newBlock.style = { fill: '#e5e7eb', couleur: '#94a3b8', epaisseur: 1, borderRadius: 4 };
        newBlock.largeurBox = 150;
        newBlock.hauteurBox = 100;
        break;
      case 'cercle':
        newBlock.style = { fill: '#e5e7eb', couleur: '#94a3b8', epaisseur: 1 };
        newBlock.largeurBox = 100;
        newBlock.hauteurBox = 100;
        break;
      case 'qrcode':
        newBlock.url = `{{${this.generateVariableName('qrcode')}}}`;
        newBlock.largeurBox = 100;
        newBlock.hauteurBox = 100;
        break;
      case 'codebarre':
        newBlock.url = `{{${this.generateVariableName('codebarre')}}}`;
        newBlock.largeurBox = 160;
        newBlock.hauteurBox = 60;
        break;
      case 'signature':
        newBlock.largeurBox = 180;
        newBlock.hauteurBox = 70;
        break;
      case 'graphique':
        newBlock.source = `{{${this.generateVariableName('graphique')}}}`;
        newBlock.largeurBox = 300;
        newBlock.hauteurBox = 180;
        break;
    }

    this.clampBlockToUsableArea(newBlock);
    this.blocks.push(newBlock);
    this.afterPagesChanged();
  }

  removeBlock(index: number): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
    this.blocks.splice(index, 1);
    this.afterPagesChanged();
  }

  moveBlock(index: number, direction: 'up' | 'down'): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
    const newIndex = direction === 'up' ? index - 1 : index + 1;
    if (newIndex < 0 || newIndex >= this.blocks.length) return;
    [this.blocks[index], this.blocks[newIndex]] = [this.blocks[newIndex], this.blocks[index]];
    this.afterPagesChanged();
  }

  selectBlock(block: DesignBlock): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
    this.selectedBlock = block;
  }

  updateBlock(updated: DesignBlock): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
    this.clampBlockToUsableArea(updated);
    const index = this.blocks.findIndex(b => b.id === updated.id);
    if (index !== -1) {
      const newBlocks = [...this.blocks];
      newBlocks[index] = { ...updated };
      this.blocks = newBlocks;
      this.selectedBlock = newBlocks[index];
      this.afterPagesChanged();
    }
  }

  onDrop(event: CdkDragDrop<DesignBlock[]>): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
    if (event.previousIndex === event.currentIndex) return;
    moveItemInArray(this.blocks, event.previousIndex, event.currentIndex);
    this.afterPagesChanged();
  }

  onCanvasBlocksChange(newBlocks: DesignBlock[]): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
    this.blocks = newBlocks;
    if (this.selectedBlock) {
      const matching = newBlocks.find(b => b.id === this.selectedBlock?.id);
      this.selectedBlock = matching || null;
    }
    this.afterPagesChanged();
  }

  onToggleVisibility(block: DesignBlock): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
    block.visible = !block.visible;
    this.blocks = [...this.blocks];
    this.afterPagesChanged();
  }

  onToggleLock(block: DesignBlock): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
    block.locked = !block.locked;
    this.blocks = [...this.blocks];
    this.afterPagesChanged();
  }

  onLayersReordered(newBlocks: DesignBlock[]): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
    this.blocks = newBlocks;
    this.afterPagesChanged();
  }

  // ---------- Multi-sélection déléguée ----------
  get selectedBlockIds(): string[] {
    return this.selectionService.selectedBlockIds();
  }

  alignSelectedBlocks(alignment: 'left' | 'centerH' | 'right' | 'top' | 'bottom'): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
    this.selectionService.alignSelected(this.blocks, alignment, b => this.clampBlockToUsableArea(b));
    this.afterPagesChanged();
  }

  distributeSelectedBlocks(direction: 'horizontal' | 'vertical'): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
    this.selectionService.distributeSelected(this.blocks, direction, b => this.clampBlockToUsableArea(b));
    this.afterPagesChanged();
  }

  onCanvasSelectionChange(ids: string[]): void {
    this.selectionService.setSelectionIds(ids, this.blocks);
    this.selectedBlock = this.selectionService.selectedBlock();
  }

  onCanvasBlockSelected(block: DesignBlock | null): void {
    this.selectedBlock = block;
    this.selectionService.select(block, false, this.blocks);
  }

  // ---------- Presse-papier délégué ----------
  get canCopy(): boolean {
    return this.clipboardService.canCopy(this.isLocked, this.selectedBlock);
  }

  get canPaste(): boolean {
    return this.clipboardService.canPaste(this.isLocked);
  }

  copySelectedBlock(): void {
    if (!this.selectedBlock || this.isLocked) return;
    this.clipboardService.copy(this.selectedBlock);
  }

  pasteBlock(): void {
    if (this.isLocked) return;
    const pasted = this.clipboardService.paste(b => this.clampBlockToUsableArea(b));
    if (pasted) {
      this.blocks.push(pasted);
      this.selectedBlock = pasted;
      this.selectionService.select(pasted, false, this.blocks);
      this.afterPagesChanged();
    }
  }

  duplicateSelectedBlock(): void {
    if (!this.selectedBlock || this.isLocked) return;
    const duplicated = this.clipboardService.duplicate(this.selectedBlock, b => this.clampBlockToUsableArea(b));
    this.blocks.push(duplicated);
    this.selectedBlock = duplicated;
    this.selectionService.select(duplicated, false, this.blocks);
    this.afterPagesChanged();
  }

  deleteSelectedBlock(): void {
    if (!this.selectedBlock || this.isLocked) return;
    const index = this.blocks.findIndex(b => b.id === this.selectedBlock?.id);
    if (index !== -1) {
      this.removeBlock(index);
      this.selectedBlock = null;
      this.selectionService.clear();
    }
  }

  // ---------- Insertion de Variables & Actions Métier ----------
  insertVariableBlock(variable: Variable): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
    const area = this.usableAreaPx;
    const isImage = variable.type === 'IMAGE';
    const newBlock: DesignBlock = {
      id: crypto.randomUUID(),
      type: isImage ? 'image' : 'texte',
      contenu: isImage ? '' : `{{${variable.nomVariable}}}`,
      url: isImage ? `{{${variable.nomVariable}}}` : undefined,
      style: { fontSize: 12 },
      x: area.minX + 20,
      y: area.minY + 20,
      visible: true,
      locked: false,
      rotation: 0,
    };
    this.clampBlockToUsableArea(newBlock);
    this.blocks.push(newBlock);
    this.selectedBlock = newBlock;
    this.selectionService.select(newBlock, false, this.blocks);
    this.afterPagesChanged();
  }

  startOnboarding(): void {
    this.onboardingService.startTour(true);
  }

  requestDuplicate(): void { this.duplicateRequest.emit(); }
  manualSave(): void { this.manualSaveRequest.emit(); }
  preview(): void { this.previewRequest.emit(); }
  publish(): void { this.publishRequest.emit(); }

  exportPdf(): void {
    if (this.isLocked || this.fillingMode) {
      this.exportRequest.emit(this.fillerData.getValues());
    } else {
      this.exportRequest.emit();
    }
  }

  exportHtml(): void {
    if (!this.templateId) return;
    if (!this.isLocked && this.templateStatus !== 'PUBLIE') {
      alert('Le template doit être publié avant de générer un aperçu HTML.');
      return;
    }
    this.api.getSchema(this.templateId).subscribe({
      next: (schema) => {
        const mockData = this.mockDataService.generate(this.allBlocksFlat);
        const fillData = this.fillingMode ? this.fillerData.getValues() : {};
        const rawData = { ...mockData, ...fillData };
        const corrected: Record<string, any> = {};
        for (const v of schema.variables) {
          const raw = rawData[v.nomVariable];
          if (v.type === 'FLOAT') {
            const num = Number(raw);
            corrected[v.nomVariable] = (raw == null || raw === '' || isNaN(num)) ? 0 : num;
          } else if (v.type === 'BOOLEAN') {
            corrected[v.nomVariable] = !!raw;
          } else if (v.type === 'ARRAY') {
            corrected[v.nomVariable] = Array.isArray(raw) ? raw : [];
          } else {
            corrected[v.nomVariable] = raw || '';
          }
        }
        this.api.exportHtml(this.templateId!, corrected).subscribe({
          next: (blob) => window.open(window.URL.createObjectURL(blob), '_blank'),
          error: () => alert("Échec de l'export HTML.")
        });
      },
      error: () => alert('Impossible de charger le schéma du template.')
    });
  }

  onExplicitVariablesChanged(vars: Variable[]): void {
    queueMicrotask(() => {
      this.explicitVariables = vars;
      this.cdr.markForCheck();
    });
  }
  onFillerGenerate(data: Record<string, any>): void { this.exportRequest.emit(data); }

  get statusDimensions(): string {
    if (!this.selectedBlock) return '—';
    return `${this.selectedBlock.largeurBox || 200} × ${this.selectedBlock.hauteurBox || 50} px`;
  }

  get statusPosition(): string {
    if (!this.selectedBlock) return '—';
    return `X: ${this.selectedBlock.x || 0}  Y: ${this.selectedBlock.y || 0}`;
  }

  getBlockSummary(block: DesignBlock): string {
    if (block.type === 'tableau') return `Tableau (${block.colonnes ? block.colonnes.length : 0} colonnes)`;
    if (block.type === 'ligne') return `Ligne (${block.style?.epaisseur || 1}px)`;
    if (block.type === 'image') return block.url || 'Aucune URL';
    return block.contenu || '(vide)';
  }

  private generateVariableName(prefix: string): string {
    const count = (this.variableCounters[prefix] || 0) + 1;
    this.variableCounters[prefix] = count;
    return `${prefix}_${count}`;
  }

  // ---------- Raccourcis Clavier ----------
  @HostListener('window:keydown', ['$event'])
  handleKeyboardShortcuts(event: KeyboardEvent): void {
    const target = event.target as HTMLElement;
    if (target && ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName)) return;

    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'z' && !event.shiftKey) {
      event.preventDefault();
      this.undo();
    } else if (
      ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'y') ||
      ((event.ctrlKey || event.metaKey) && event.shiftKey && event.key.toLowerCase() === 'z')
    ) {
      event.preventDefault();
      this.redo();
    } else if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'c') {
      if (this.canCopy) { event.preventDefault(); this.copySelectedBlock(); }
    } else if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'v') {
      if (this.canPaste) { event.preventDefault(); this.pasteBlock(); }
    } else if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'd') {
      if (this.canCopy) { event.preventDefault(); this.duplicateSelectedBlock(); }
    } else if (event.key === 'Delete' || event.key === 'Backspace') {
      if (this.selectedBlock && !this.isLocked) { event.preventDefault(); this.deleteSelectedBlock(); }
    }
  }
}