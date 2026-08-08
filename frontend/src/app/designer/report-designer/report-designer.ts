import { Component, EventEmitter, Input, Output } from '@angular/core';
import { DesignBlock } from '../models/design-block.model';
import { CommonModule, UpperCasePipe } from '@angular/common';
import { BlockEditor } from "../block-editor/block-editor";
import { BlockPreview } from '../block-preview/block-preview';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { DesignCanvas } from '../design-canvas/design-canvas';
import { Sidebar } from '../sidebar/sidebar';
import { FormsModule } from '@angular/forms';
import {
  LucideAngularModule,
  Undo2,
  Redo2,
  Grid3x3,
  Magnet,
  Ruler,
  Minus,
  Plus,
  Eye,
  Download,
  Rocket,
  Share2,
  Save,
  FileText,
  Check,
  Loader2,
  Ruler as RulerIcon,
  Edit
} from 'lucide-angular';
import { Variable } from '../../models/variable.model';
import { TemplateFiller } from "../template-filler/template-filler";
import { FillerDataService } from '../services/filler-data';

@Component({
  selector: 'app-report-designer',
  standalone: true,
  imports: [
    UpperCasePipe,
    CommonModule,
    BlockEditor,
    BlockPreview,
    DragDropModule,
    DesignCanvas,
    Sidebar,
    FormsModule,
    LucideAngularModule,
    TemplateFiller
],
  templateUrl: './report-designer.html',
  styleUrl: './report-designer.scss',
})
export class ReportDesigner {
  @Input() blocks: DesignBlock[] = [];
  @Input() templateName = '';
  @Input() savingStatus: 'idle' | 'saving' | 'saved' = 'idle';
  @Input() templateId: string | null = null;
  @Output() blocksChange = new EventEmitter<DesignBlock[]>();
  @Output() publishRequest = new EventEmitter<void>();
  @Output() exportRequest = new EventEmitter<Record<string, any> | undefined>();
  @Output() previewRequest = new EventEmitter<void>();

  selectedBlock: DesignBlock | null = null;
  explicitVariables: Variable[] = [];

  showGrid = false;
  snapEnabled = false;
  showGuides = false;
  zoomPercent = 100;
  paperFormat = 'A4';
  fillingMode = false;
  
  // Historique pour undo/redo
  private undoStack: DesignBlock[][] = [];
  private redoStack: DesignBlock[][] = [];
  private isUndoRedoAction = false; // pour ne pas empiler pendant undo/redo

  constructor(private fillerData: FillerDataService) {}

  private paperDimensions: Record<string, { width: number; height: number }> = {
    'A4': { width: 794, height: 1123 },
    'A5': { width: 559, height: 794 },
    'Letter': { width: 816, height: 1056 },
    'Legal': { width: 816, height: 1344 }
  };

  // Icônes exposées au template
  readonly icons = {
    undo: Undo2,
    redo: Redo2,
    grid: Grid3x3,
    snap: Magnet,
    guides: Ruler,
    zoomOut: Minus,
    zoomIn: Plus,
    preview: Eye,
    export: Download,
    publish: Rocket,
    share: Share2,
    save: Save,
    file: FileText,
    check: Check,
    loader: Loader2,
    Ruler: RulerIcon,
    edit: Edit,
    fileText: FileText
  };

  private variableCounters: Record<string, number> = {};

  @Output() manualSaveRequest = new EventEmitter<void>();

  manualSave(): void {
    this.manualSaveRequest.emit();
  }

  toggleFillingMode(): void {
    this.fillingMode = !this.fillingMode;
  }

  ngOnInit() {
    // Enregistrer l'état initial dans l'historique
    this.pushUndoState();
  }

  //Reception et transmission
  onExplicitVariablesChanged(vars: Variable[]): void {
    this.explicitVariables = vars;
  }

  exportPdf(): void {
    if (this.fillingMode) {
      // Utilise les valeurs réelles
      const data = this.fillerData.getValues();
      this.exportRequest.emit(data);   // adaptez l'événement si nécessaire
    } else {
      // Utilise les mocks (comportement actuel)
      this.exportRequest.emit();
    }
  }

  // ---- Gestion de l'historique ----
  private pushUndoState() {
    if (this.isUndoRedoAction) return;
    // Clone profond des blocs (on peut utiliser JSON.parse(JSON.stringify) pour la simplicité)
    const cloned = JSON.parse(JSON.stringify(this.blocks));
    this.undoStack.push(cloned);
    // Limiter la pile à 50 états
    if (this.undoStack.length > 50) {
      this.undoStack.shift();
    }
    // Vider la pile redo après une nouvelle action
    this.redoStack = [];
  }

  // Nouvelle méthode utilitaire pour la barre d'état
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

  private restoreState(state: DesignBlock[]) {
    this.isUndoRedoAction = true;
    this.blocks = state;
    this.blocksChange.emit([...this.blocks]);
    this.isUndoRedoAction = false;
  }


  addBlock(type: DesignBlock['type']): void {
    const newBlock: DesignBlock = {
      id: crypto.randomUUID(),
      type,
      contenu: '',
      style: {},
      x: 50,
      y: 50 + this.blocks.length * 80,
      visible: true,
      locked: false
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
          ['', ''],
          ['', '']
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
      // --- Nouveaux types (bibliothèque étendue) ---
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
    this.blocksChange.emit([...this.blocks]);
  }

  removeBlock(index: number): void {
    this.blocks.splice(index, 1);
    this.afterBlocksChanged();
  }

  moveBlock(index: number, direction: 'up' | 'down'): void {
    const newIndex = direction === 'up' ? index - 1 : index + 1;
    if (newIndex < 0 || newIndex >= this.blocks.length) return;
    [this.blocks[index], this.blocks[newIndex]] = [this.blocks[newIndex], this.blocks[index]];
    this.afterBlocksChanged();
  }

  getBlockSummary(block: DesignBlock): string {
    if (block.type === 'tableau') {
      const nbColonnes = block.colonnes ? block.colonnes.length : 0;
      return `Tableau (${nbColonnes} colonnes)`;
    }
    if (block.type === 'ligne') {
      return `Ligne (${block.style?.epaisseur || 1}px)`;
    }
    if (block.type === 'image') {
      return block.url || 'Aucune URL';
    }
    return block.contenu || '(vide)';
  }

  selectBlock(block: DesignBlock): void {
    this.selectedBlock = block;
  }

  updateBlock(updated: DesignBlock): void {
    const index = this.blocks.findIndex(b => b.id === updated.id);
    if (index !== -1) {
      this.blocks[index] = { ...updated };
      this.blocks = [...this.blocks];
      this.selectedBlock = this.blocks[index];  // ← ajoute cette ligne
      this.blocksChange.emit([...this.blocks]);
    }
  }

  onDrop(event: CdkDragDrop<DesignBlock[]>): void {
    if (event.previousIndex === event.currentIndex) return;
    moveItemInArray(this.blocks, event.previousIndex, event.currentIndex);
    this.afterBlocksChanged();
  }

  onCanvasBlocksChange(newBlocks: DesignBlock[]): void {
    this.blocks = newBlocks;
    this.afterBlocksChanged();
  }

  private generateVariableName(type: string): string {
    if (!this.variableCounters[type]) {
      this.variableCounters[type] = 0;
    }
    this.variableCounters[type]++;
    return `${type}_${this.variableCounters[type]}`;
  }

  onToggleVisibility(block: DesignBlock): void {
    block.visible = !block.visible;
    this.blocks = [...this.blocks];
    this.afterBlocksChanged();
  }

  onToggleLock(block: DesignBlock): void {
    block.locked = !block.locked;
    this.blocks = [...this.blocks];
    this.afterBlocksChanged();
  }

  undo(): void {
    if (this.undoStack.length <= 1) return; // Le dernier état est l'état actuel
    // L'état actuel va dans redo
    const current = this.undoStack.pop()!;
    this.redoStack.push(current);
    const previous = this.undoStack[this.undoStack.length - 1];
    this.restoreState(previous);
  }

  redo(): void {
    if (this.redoStack.length === 0) return;
    const next = this.redoStack.pop()!;
    this.undoStack.push(next);
    this.restoreState(next);
  }

  private afterBlocksChanged() {
    if (!this.isUndoRedoAction) {
      this.pushUndoState();
    }
    this.blocksChange.emit([...this.blocks]);
  }

  toggleGrid(): void {
    this.showGrid = !this.showGrid;
  }

  toggleSnap(): void {
    this.snapEnabled = !this.snapEnabled;
  }

  toggleGuides(): void {
    this.showGuides = !this.showGuides;
  }

  zoomIn(): void {
    this.zoomPercent = Math.min(200, this.zoomPercent + 10);
  }

  zoomOut(): void {
    this.zoomPercent = Math.max(50, this.zoomPercent - 10);
  }

  get canvasWidth(): number {
    return this.paperDimensions[this.paperFormat]?.width || 794;
  }

  get canvasHeight(): number {
    return this.paperDimensions[this.paperFormat]?.height || 1123;
  }

  onPaperFormatChange(): void {
    // TODO: changer les dimensions du canevas
  }

  preview(): void {
    this.previewRequest.emit();
  }

  publish(): void {
    this.publishRequest.emit();
  }
}