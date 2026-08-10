import { Component, EventEmitter, Input, Output, OnInit, SimpleChanges } from '@angular/core';
import { DesignBlock, DesignPage } from '../models/design-block.model';
import { CommonModule, UpperCasePipe } from '@angular/common';
import { BlockEditor } from "../block-editor/block-editor";
import { BlockPreview } from '../block-preview/block-preview';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { DesignCanvas } from '../design-canvas/design-canvas';
import { Sidebar } from '../sidebar/sidebar';
import { FormsModule } from '@angular/forms';
import {
  LucideAngularModule, Undo2, Redo2, Grid3x3, Magnet, Ruler, Minus, Plus, Eye,
  Download, Rocket, Share2, Save, FileText, Check, Loader2, Ruler as RulerIcon,
  Edit, X, Lock, Copy
} from 'lucide-angular';
import { Variable } from '../../models/variable.model';
import { TemplateFiller } from "../template-filler/template-filler";
import { FillerDataService } from '../services/filler-data';
import { MockDataService } from '../services/mock-data.service';
import { TemplateApiService } from '../../services/template-api';

@Component({
  selector: 'app-report-designer',
  standalone: true,
  imports: [
    UpperCasePipe, CommonModule, BlockEditor, BlockPreview, DragDropModule,
    DesignCanvas, Sidebar, FormsModule, LucideAngularModule, TemplateFiller
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

  @Output() pagesChange = new EventEmitter<DesignPage[]>();
  @Output() publishRequest = new EventEmitter<void>();
  @Output() exportRequest = new EventEmitter<Record<string, any> | undefined>();
  @Output() previewRequest = new EventEmitter<void>();
  @Output() manualSaveRequest = new EventEmitter<void>();
  @Output() duplicateRequest = new EventEmitter<void>();

  selectedBlock: DesignBlock | null = null;
  explicitVariables: Variable[] = [];

  activePageIndex = 0;
  editingPageIndex: number | null = null;

  showGrid = false;
  snapEnabled = false;
  showGuides = false;
  zoomPercent = 100;
  paperFormat = 'A4';
  fillingMode = false;

  lockedMessage: string | null = null;
  private lockedMessageTimer: any;

  private undoStack: DesignPage[][] = [];
  private redoStack: DesignPage[][] = [];
  private isUndoRedoAction = false;
  private ignoreNextChange = false;

  constructor(
    private api: TemplateApiService,
    private fillerData: FillerDataService,
    private mockDataService: MockDataService
  ) {}

  private paperDimensions: Record<string, { width: number; height: number }> = {
    'A4': { width: 794, height: 1123 },
    'A5': { width: 559, height: 794 },
    'Letter': { width: 816, height: 1056 },
    'Legal': { width: 816, height: 1344 }
  };

  readonly icons = {
    undo: Undo2, redo: Redo2, grid: Grid3x3, snap: Magnet, guides: Ruler,
    zoomOut: Minus, zoomIn: Plus, preview: Eye, export: Download, publish: Rocket,
    share: Share2, save: Save, file: FileText, check: Check, loader: Loader2,
    Ruler: RulerIcon, edit: Edit, fileText: FileText, close: X, lock: Lock, copy: Copy
  };

  private variableCounters: Record<string, number> = {};

  // ---------- Accès aux blocs de la page active ----------

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

  get isLocked(): boolean {
    return this.templateStatus !== 'BROUILLON';
  }

  ngOnInit(): void {
    if (!this.pages || this.pages.length === 0) {
      this.pages = [{ id: crypto.randomUUID(), nom: 'Page 1', blocks: [] }];
    }
    this.pushUndoState();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['pages'] && !changes['pages'].firstChange) {
      if (this.ignoreNextChange) {
        this.ignoreNextChange = false;
        return;
      }
      this.activePageIndex = 0;
      this.undoStack = [];
      this.redoStack = [];
      this.pushUndoState();
    }
  }

  showLockedMessage(): void {
    this.lockedMessage = 'Ce modèle est publié et ne peut plus être modifié.';
    clearTimeout(this.lockedMessageTimer);
    this.lockedMessageTimer = setTimeout(() => (this.lockedMessage = null), 4000);
  }

  requestDuplicate(): void {
    this.duplicateRequest.emit();
  }

  toggleFillingMode(): void {
    this.fillingMode = !this.fillingMode;
  }

  onExplicitVariablesChanged(vars: Variable[]): void {
    this.explicitVariables = vars;
  }

  onFillerGenerate(data: Record<string, any>): void {
    this.exportRequest.emit(data);
  }

  exportPdf(): void {
    if (this.isLocked) {
      this.exportRequest.emit(this.fillerData.getValues());
      return;
    }
    if (this.fillingMode) {
      this.exportRequest.emit(this.fillerData.getValues());
    } else {
      this.exportRequest.emit();
    }
  }

  manualSave(): void {
    this.manualSaveRequest.emit();
  }

  // ---------- Pages ----------

  addPage(): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
    const newPage: DesignPage = { id: crypto.randomUUID(), nom: `Page ${this.pages.length + 1}`, blocks: [] };
    this.pages = [...this.pages, newPage];
    this.activePageIndex = this.pages.length - 1;
    this.selectedBlock = null;
    this.afterPagesChanged();
  }

  removePage(index: number, event: Event): void {
    event.stopPropagation();
    if (this.isLocked) { this.showLockedMessage(); return; }
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
    const copy: DesignPage = { id: crypto.randomUUID(), nom: `${source.nom} (copie)`, blocks: clonedBlocks };
    this.pages = [...this.pages.slice(0, index + 1), copy, ...this.pages.slice(index + 1)];
    this.activePageIndex = index + 1;
    this.afterPagesChanged();
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

  // ---------- Blocs ----------

  addBlock(type: DesignBlock['type']): void {
    if (this.isLocked) { this.showLockedMessage(); return; }

    const newBlock: DesignBlock = {
      id: crypto.randomUUID(), type, contenu: '', style: {},
      x: 50, y: 50, visible: true, locked: false
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
        newBlock.lignes = [
          [{ value: '' }, { value: '' }],
          [{ value: '' }, { value: '' }]
        ];
        delete newBlock.source;
        delete newBlock.colonnes;
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

  getBlockSummary(block: DesignBlock): string {
    if (block.type === 'tableau') {
      const nbColonnes = block.colonnes ? block.colonnes.length : 0;
      return `Tableau (${nbColonnes} colonnes)`;
    }
    if (block.type === 'ligne') return `Ligne (${block.style?.epaisseur || 1}px)`;
    if (block.type === 'image') return block.url || 'Aucune URL';
    return block.contenu || '(vide)';
  }

  selectBlock(block: DesignBlock): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
    this.selectedBlock = block;
  }

  updateBlock(updated: DesignBlock): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
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
    this.afterPagesChanged();
  }

  private generateVariableName(type: string): string {
    if (!this.variableCounters[type]) this.variableCounters[type] = 0;
    this.variableCounters[type]++;
    return `${type}_${this.variableCounters[type]}`;
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

  // ---------- Historique ----------

  private pushUndoState(): void {
    if (this.isUndoRedoAction) return;
    const cloned = JSON.parse(JSON.stringify(this.pages));
    this.undoStack.push(cloned);
    if (this.undoStack.length > 50) this.undoStack.shift();
    this.redoStack = [];
  }

  private restoreState(state: DesignPage[]): void {
    this.isUndoRedoAction = true;
    this.pages = state;
    if (this.activePageIndex >= this.pages.length) this.activePageIndex = this.pages.length - 1;
    this.emitPagesChange();
    this.isUndoRedoAction = false;
  }

  undo(): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
    if (this.undoStack.length <= 1) return;
    const current = this.undoStack.pop()!;
    this.redoStack.push(current);
    const previous = this.undoStack[this.undoStack.length - 1];
    this.restoreState(previous);
  }

  redo(): void {
    if (this.isLocked) { this.showLockedMessage(); return; }
    if (this.redoStack.length === 0) return;
    const next = this.redoStack.pop()!;
    this.undoStack.push(next);
    this.restoreState(next);
  }

  private afterPagesChanged(): void {
    if (!this.isUndoRedoAction) this.pushUndoState();
    this.emitPagesChange();
  }

  private emitPagesChange(): void {
    this.ignoreNextChange = true;
    this.pagesChange.emit([...this.pages]);
  }

  // ---------- Toolbar ----------

  get statusDimensions(): string {
    if (!this.selectedBlock) return '—';
    const w = this.selectedBlock.largeurBox || 200;
    const h = this.selectedBlock.hauteurBox || 50;
    return `${w} × ${h} px`;
  }

  get statusPosition(): string {
    if (!this.selectedBlock) return '—';
    return `X: ${this.selectedBlock.x || 0}  Y: ${this.selectedBlock.y || 0}`;
  }

  exportHtml(): void {
    if (!this.templateId) return;

    if (this.isLocked === false && this.templateStatus !== 'PUBLIE') {
      alert('Le template doit être publié avant de générer un aperçu HTML.');
      return;
    }

    this.api.getSchema(this.templateId).subscribe({
      next: (schema) => {
        const mockData = this.mockDataService.generate(this.allBlocksFlat);
        const fillData = this.fillingMode ? this.fillerData.getValues() : {};
        const rawData = { ...mockData, ...fillData };

        const correctedData: Record<string, any> = {};
        for (const v of schema.variables) {
          const raw = rawData[v.nomVariable];
          if (v.type === 'FLOAT') {
            const num = Number(raw);
            correctedData[v.nomVariable] = (raw == null || raw === '' || isNaN(num)) ? 0 : num;
          } else if (v.type === 'BOOLEAN') {
            correctedData[v.nomVariable] = !!raw;
          } else if (v.type === 'ARRAY') {
            correctedData[v.nomVariable] = Array.isArray(raw) ? raw : [];
          } else {
            correctedData[v.nomVariable] = raw || '';
          }
        }

        this.api.exportHtml(this.templateId!, correctedData).subscribe({
          next: (blob) => {
            const url = window.URL.createObjectURL(blob);
            window.open(url, '_blank');
          },
          error: (err) => {
            console.error('Erreur export HTML', err);
            if (err.status === 400 && err.error instanceof Blob) {
              err.error.text().then((text: string) => {
                try {
                  const body = JSON.parse(text);
                  alert('Échec de l\'export HTML : ' + (body.message || 'Erreur de validation.'));
                } catch {
                  alert('Échec de l\'export HTML (erreur 400).');
                }
              });
            } else {
              alert('Échec de l\'export HTML. Voir la console pour le détail.');
            }
          }
        });
      },
      error: (err) => {
        console.error('Erreur chargement du schéma', err);
        alert('Impossible de charger le schéma du template (est-il publié ?).');
      }
    });
  }

  toggleGrid(): void { this.showGrid = !this.showGrid; }
  toggleSnap(): void { this.snapEnabled = !this.snapEnabled; }
  toggleGuides(): void { this.showGuides = !this.showGuides; }
  zoomIn(): void { this.zoomPercent = Math.min(200, this.zoomPercent + 10); }
  zoomOut(): void { this.zoomPercent = Math.max(50, this.zoomPercent - 10); }

  get canvasWidth(): number { return this.paperDimensions[this.paperFormat]?.width || 794; }
  get canvasHeight(): number { return this.paperDimensions[this.paperFormat]?.height || 1123; }

  onPaperFormatChange(): void {}

  preview(): void { this.previewRequest.emit(); }
  publish(): void { this.publishRequest.emit(); }
}