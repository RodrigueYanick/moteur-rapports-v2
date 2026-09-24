import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnDestroy,
  ChangeDetectorRef,
  ElementRef,
  ViewChild,
  HostListener,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FillerDataService } from '../services/filler-data';
import { Subscription } from 'rxjs';
import { LucideAngularModule, ImageOff, QrCode, Barcode, PenTool, Table2 } from 'lucide-angular';
import { DesignBlock, BLOCK_DEFAULT_DIMENSIONS } from '../models/design-block.model';

import { FormsModule } from '@angular/forms';
import { VariableAutocompleteDirective } from '../../shared/directives/variable-autocomplete.directive';
import { Variable } from '../../models/variable.model';

@Component({
  selector: 'app-design-canvas',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, FormsModule, VariableAutocompleteDirective],
  templateUrl: './design-canvas.html',
  styleUrls: ['./design-canvas.scss'],
})
export class DesignCanvas implements OnInit, OnDestroy {
  @ViewChild('canvasContainer', { static: true }) canvasContainer?: ElementRef<HTMLDivElement>;

  @Input() blocks: DesignBlock[] = [];
  @Input() explicitVariables: Variable[] = [];
  @Input() zoom: number = 100;
  @Input() canvasHeight: number = 1123;
  @Input() canvasWidth: number = 794;
  @Input() showGrid = false;
  @Input() snapEnabled = false;
  @Input() showGuides = false;
  @Input() locked = false;
  @Input() margeHautPx: number = 0;
  @Input() margeBasPx: number = 0;
  @Input() margeGauchePx: number = 0;
  @Input() margeDroitePx: number = 0;
  @Input() couleurFond: string = '#ffffff';

  @Input() headerActif: boolean = false;
  @Input() hauteurHeaderMm: number = 15;
  @Input() headerContenu?: string | null = null;
  @Input() headerAlignement: string = 'GAUCHE';
  @Input() headerLigneSeparation: boolean = true;
  @Input() headerCouleurLigne: string = '#cccccc';

  @Input() footerActif: boolean = false;
  @Input() hauteurFooterMm: number = 12;
  @Input() footerContenu?: string | null = null;
  @Input() footerAlignement: string = 'CENTRE';
  @Input() footerLigneSeparation: boolean = true;
  @Input() footerCouleurLigne: string = '#cccccc';

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

  @Output() lockedInteraction = new EventEmitter<void>();
  @Output() blocksPreviewChange = new EventEmitter<DesignBlock[]>();
  @Output() blocksChange = new EventEmitter<DesignBlock[]>();
  @Output() blockSelected = new EventEmitter<DesignBlock>();
  @Output() selectionChange = new EventEmitter<string[]>();

  private valuesSubscription?: Subscription;
  @Input() selectedBlockIds: string[] = [];

  // État du drag manuel
  // --- Édition directe de texte (In-Place) ---
  editingBlockId: string | null = null;
  inPlaceValue: string = '';

  // --- Guides d'alignement intelligents (Smart Guides) ---
  activeVerticalGuides: number[] = [];
  activeHorizontalGuides: number[] = [];

  // --- Sélection élastique au lasso (Marquee) ---
  isMarquee = false;
  marqueeStart = { x: 0, y: 0 };
  marqueeRect: { x: number; y: number; w: number; h: number } | null = null;

  // État du drag groupé manuel
  private dragState: {
    blockId: string;
    startClientX: number;
    startClientY: number;
    startBlockX: number;
    startBlockY: number;
    startBlocks: Map<string, { x: number; y: number; w: number; h: number }>;
    moved: boolean;
  } | null = null;

  // Flag pour empêcher la sélection après un drag
  private wasDragging = false;
  private lastBlockMouseDownTime = 0;

  get selectedBlockId(): string | null {
    return this.selectedBlockIds[this.selectedBlockIds.length - 1] ?? null;
  }

  // ---------- Zone utilisable (hors marges, hors header, hors footer) ----------

  /** Retourne les bornes de la zone utilisable en pixels */
  get usableArea(): { minX: number; minY: number; maxX: number; maxY: number } {
    return {
      minX: this.safeMargeGauchePx,
      minY: this.safeMargeHautPx + this.headerHeightPx,
      maxX: this.canvasWidth - this.safeMargeDroitePx,
      maxY: this.canvasHeight - this.safeMargeBasPx - this.footerHeightPx,
    };
  }

  get contentAreaStyle(): { [key: string]: string } {
    const left = this.safeMargeGauchePx;
    const top = this.safeMargeHautPx + this.headerHeightPx;
    const width = this.canvasWidth - this.safeMargeGauchePx - this.safeMargeDroitePx;
    const height = this.canvasHeight - top - this.safeMargeBasPx - this.footerHeightPx;
    return {
      'left': `${left}px`,
      'top': `${top}px`,
      'width': `${width}px`,
      'height': `${Math.max(0, height)}px`,
    };
  }

  get headerZoneStyle(): { [key: string]: string } {
    return {
      'left': `${this.safeMargeGauchePx}px`,
      'top': `${this.safeMargeHautPx}px`,
      'width': `${this.canvasWidth - this.safeMargeGauchePx - this.safeMargeDroitePx}px`,
      'height': `${this.headerHeightPx}px`,
      'border-bottom': this.headerLigneSeparation ? `1px solid ${this.headerCouleurLigne || '#cccccc'}` : '1px dashed #cbd5e1'
    };
  }

  get footerZoneStyle(): { [key: string]: string } {
    const top = this.canvasHeight - this.safeMargeBasPx - this.footerHeightPx;
    return {
      'left': `${this.safeMargeGauchePx}px`,
      'top': `${top}px`,
      'width': `${this.canvasWidth - this.safeMargeGauchePx - this.safeMargeDroitePx}px`,
      'height': `${this.footerHeightPx}px`,
      'border-top': this.footerLigneSeparation ? `1px solid ${this.footerCouleurLigne || '#cccccc'}` : '1px dashed #cbd5e1'
    };
  }

  get marginTopStyle(): { [key: string]: string } {
    return {
      'left': '0',
      'top': '0',
      'width': `${this.canvasWidth}px`,
      'height': `${this.safeMargeHautPx}px`,
    };
  }

  get marginBottomStyle(): { [key: string]: string } {
    return {
      'left': '0',
      'bottom': '0',
      'width': `${this.canvasWidth}px`,
      'height': `${this.safeMargeBasPx}px`,
    };
  }

  get marginLeftStyle(): { [key: string]: string } {
    const topOffset = this.safeMargeHautPx;
    const height = this.canvasHeight - this.safeMargeHautPx - this.safeMargeBasPx;
    return {
      'left': '0',
      'top': `${topOffset}px`,
      'width': `${this.safeMargeGauchePx}px`,
      'height': `${Math.max(0, height)}px`,
    };
  }

  get marginRightStyle(): { [key: string]: string } {
    const topOffset = this.safeMargeHautPx;
    const height = this.canvasHeight - this.safeMargeHautPx - this.safeMargeBasPx;
    return {
      'right': '0',
      'top': `${topOffset}px`,
      'width': `${this.safeMargeDroitePx}px`,
      'height': `${Math.max(0, height)}px`,
    };
  }

  /**
   * Contraint la position d'un bloc pour qu'il reste entièrement
   * dans la zone utilisable (hors marges).
   * Modifie le bloc en place et le retourne.
   */
  clampBlockPosition(block: DesignBlock): DesignBlock {
    const area = this.usableArea;
    const fallback = BLOCK_DEFAULT_DIMENSIONS[block.type];
    const w = block.largeurBox || fallback.w;
    const h = block.hauteurBox || fallback.h;

    // Largeur maximale disponible dans la zone utilisable
    const maxW = area.maxX - area.minX;
    const maxH = area.maxY - area.minY;

    // Si le bloc est plus grand que la zone, on limite ses dimensions
    if (w > maxW) {
      block.largeurBox = maxW;
    }
    if (h > maxH) {
      block.hauteurBox = maxH;
    }

    const effectiveW = block.largeurBox || fallback.w;
    const effectiveH = block.hauteurBox || fallback.h;

    // Contraint la position horizontale
    const clampedX = Math.max(area.minX, Math.min(block.x || 0, area.maxX - effectiveW));

    // Pour les tableaux, on ne contraint que le haut (y >= minY)
    // car ils peuvent s'étendre sur plusieurs pages.
    let clampedY: number;
    if (block.type === 'tableau') {
      clampedY = Math.max(area.minY, block.y || 0);
    } else {
      clampedY = Math.max(area.minY, Math.min(block.y || 0, area.maxY - effectiveH));
    }

    block.x = clampedX;
    block.y = clampedY;

    return block;
  }

  readonly icons = {
    imagePlaceholder: ImageOff,
    qrcode: QrCode,
    barcode: Barcode,
    penTool: PenTool,
    table: Table2
  };

  constructor(
    private fillerData: FillerDataService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.valuesSubscription = this.fillerData.values$.subscribe(() => {
      this.cdr.detectChanges();
    });
  }

  ngOnDestroy(): void {
    this.valuesSubscription?.unsubscribe();
    // Aucun nettoyage supplémentaire car on utilise des @HostListener
  }

  // ---------- Gestion manuelle du drag & drop ----------
  // ---------- Édition directe sur la feuille (In-Place Editing) ----------

  startInPlaceEdit(block: DesignBlock, event: MouseEvent): void {
    if (this.locked || block.locked) return;
    if (block.type !== 'texte' && block.type !== 'titre') return;
    event.stopPropagation();
    event.preventDefault();
    this.editingBlockId = block.id;
    this.inPlaceValue = block.contenu || '';
    this.selectedBlockIds = [block.id];
    this.blockSelected.emit(block);
    this.selectionChange.emit(this.selectedBlockIds);
    this.cdr.detectChanges();
    setTimeout(() => {
      const textarea = this.canvasContainer?.nativeElement?.querySelector('.in-place-input') as HTMLTextAreaElement;
      if (textarea) {
        textarea.focus();
        textarea.select();
      }
    }, 10);
  }

  trackByBlockId(index: number, block: DesignBlock): string {
    return block?.id || String(index);
  }

  finishInPlaceEdit(): void {
    if (!this.editingBlockId) return;
    const blockIndex = this.blocks.findIndex(b => b.id === this.editingBlockId);
    if (blockIndex !== -1 && this.blocks[blockIndex].contenu !== this.inPlaceValue) {
      this.blocks[blockIndex].contenu = this.inPlaceValue;
      this.blocksChange.emit([...this.blocks]);
    }
    this.editingBlockId = null;
    this.cdr.detectChanges();
  }

  cancelInPlaceEdit(): void {
    this.editingBlockId = null;
    this.cdr.detectChanges();
  }

  onInPlaceKeyDown(event: KeyboardEvent): void {
    const isAutocompleteOpen = !!document.querySelector('.variable-autocomplete-dropdown');
    if ((event as any)._autocompleteHandled || (isAutocompleteOpen && ['Enter', 'Tab', 'Escape', 'ArrowDown', 'ArrowUp'].includes(event.key))) {
      return;
    }
    if (event.key === 'Escape') {
      event.preventDefault();
      event.stopPropagation();
      this.cancelInPlaceEdit();
      return;
    }
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      event.stopPropagation();
      this.finishInPlaceEdit();
      return;
    }
    event.stopPropagation();
  }

  // ---------- Sélection rectangulaire au lasso (Marquee) ----------

  private isEventInsideBlockOrInput(event: MouseEvent): boolean {
    const path = event.composedPath ? event.composedPath() : [];
    for (const item of path) {
      const el = item as HTMLElement;
      if (el.classList && (el.classList.contains('canvas-block') || el.classList.contains('in-place-input'))) {
        return true;
      }
    }
    const target = event.target as HTMLElement;
    if (target && typeof target.closest === 'function') {
      return !!(target.closest('.canvas-block') || target.closest('.in-place-input'));
    }
    return false;
  }

  onCanvasClick(event: MouseEvent): void {
    if (Date.now() - this.lastBlockMouseDownTime < 400) {
      return;
    }
    if (this.isEventInsideBlockOrInput(event)) {
      return;
    }
    this.clearSelection();
  }

  onCanvasMouseDown(event: MouseEvent): void {
    if (this.locked) return;
    if (this.isEventInsideBlockOrInput(event)) {
      return;
    }
    if (event.button !== 0) return;

    if (this.editingBlockId) {
      this.finishInPlaceEdit();
    }

    const rect = this.canvasContainer?.nativeElement.getBoundingClientRect();
    if (!rect) return;
    const scale = this.zoom / 100 || 1;
    const canvasX = (event.clientX - rect.left) / scale;
    const canvasY = (event.clientY - rect.top) / scale;

    if (!event.shiftKey && !event.ctrlKey && !event.metaKey) {
      this.clearSelection();
    }

    this.isMarquee = true;
    this.marqueeStart = { x: canvasX, y: canvasY };
    this.marqueeRect = { x: canvasX, y: canvasY, w: 0, h: 0 };
    this.cdr.detectChanges();
  }

  // ---------- Gestion manuelle du drag & drop avec Guides intelligents ----------

  /**
   * Déclenché au clic gauche sur un bloc (ou tout bouton si on ne filtre pas).
   * Initialise l'état du drag et capture les positions de départ.
   * Déclenché au clic gauche sur un bloc.
   * Initialise l'état du drag pour tous les blocs sélectionnés.
   */
  onBlockMouseDown(event: MouseEvent, block: DesignBlock): void {
    this.lastBlockMouseDownTime = Date.now();
    if (this.locked || block.locked) {
      this.lockedInteraction.emit();
      return;
    }

    // Seul le clic gauche (bouton principal) est autorisé
    if (this.editingBlockId === block.id) {
      // Permettre la sélection de texte dans le textarea sans déclencher de drag
      return;
    }
    if (this.editingBlockId && this.editingBlockId !== block.id) {
      this.finishInPlaceEdit();
    }

    if (event.button !== 0) return;

    event.stopPropagation();

    // Initialiser l'état du drag
    // Gestion de la sélection multiple avec Shift / Ctrl
    if (event.shiftKey || event.ctrlKey || event.metaKey) {
      if (this.selectedBlockIds.includes(block.id)) {
        this.setSelection(this.selectedBlockIds.filter(id => id !== block.id), block);
        return;
      } else {
        this.setSelection([...this.selectedBlockIds, block.id], block);
      }
    } else if (!this.selectedBlockIds.includes(block.id)) {
      this.setSelection([block.id], block);
    }

    // Capture des coordonnées de départ de tous les blocs sélectionnés pour le drag groupé
    const startBlocks = new Map<string, { x: number; y: number; w: number; h: number }>();
    for (const id of this.selectedBlockIds) {
      const b = this.blocks.find(x => x.id === id);
      if (b) {
        const fallback = BLOCK_DEFAULT_DIMENSIONS[b.type];
        startBlocks.set(id, {
          x: b.x || 0,
          y: b.y || 0,
          w: b.largeurBox || fallback.w,
          h: b.hauteurBox || fallback.h,
        });
      }
    }

    this.dragState = {
      blockId: block.id,
      startClientX: event.clientX,
      startClientY: event.clientY,
      startBlockX: block.x || 0,
      startBlockY: block.y || 0,
      startBlocks,
      moved: false,
    };

    // Sélectionner le bloc si ce n'est pas déjà le cas
    if (!this.selectedBlockIds.includes(block.id)) {
      this.setSelection([block.id], block);
    }
  }

  @HostListener('window:mousemove', ['$event'])
  onWindowMouseMove(event: MouseEvent): void {
    // 1. Mise à jour de la sélection au lasso
    if (this.isMarquee && this.canvasContainer?.nativeElement) {
      const rect = this.canvasContainer.nativeElement.getBoundingClientRect();
      const scale = this.zoom / 100 || 1;
      const currentX = (event.clientX - rect.left) / scale;
      const currentY = (event.clientY - rect.top) / scale;

      const x = Math.min(this.marqueeStart.x, currentX);
      const y = Math.min(this.marqueeStart.y, currentY);
      const w = Math.abs(currentX - this.marqueeStart.x);
      const h = Math.abs(currentY - this.marqueeStart.y);
      this.marqueeRect = { x, y, w, h };

      const intersectingIds: string[] = [];
      for (const b of this.blocks) {
        if (b.visible === false) continue;
        const fallback = BLOCK_DEFAULT_DIMENSIONS[b.type];
        const bw = b.largeurBox || fallback.w;
        const bh = b.hauteurBox || fallback.h;
        const bx = b.x || 0;
        const by = b.y || 0;

        // Collision rectangle AABB
        if (x < bx + bw && x + w > bx && y < by + bh && y + h > by) {
          intersectingIds.push(b.id);
        }
      }

      this.selectedBlockIds = intersectingIds;
      const lastBlock = this.blocks.find(b => b.id === intersectingIds[intersectingIds.length - 1]);
      this.blockSelected.emit(lastBlock || (null as any));
      this.selectionChange.emit(this.selectedBlockIds);
      this.cdr.detectChanges();
      return;
    }

    // 2. Déplacement de blocs avec guides intelligents
    if (!this.dragState) return;

    event.preventDefault();
    const state = this.dragState;
    const scale = this.zoom / 100 || 1;

    // Calcul du déplacement en pixels écran
    const deltaScreenX = event.clientX - state.startClientX;
    const deltaScreenY = event.clientY - state.startClientY;

    // Conversion en pixels canvas (division par le facteur de zoom)
    let deltaCanvasX = deltaScreenX / scale;
    let deltaCanvasY = deltaScreenY / scale;

    this.activeVerticalGuides = [];
    this.activeHorizontalGuides = [];

    // Aimantation intelligente (Smart Snapping) sur le bloc principal cliqué
    const primaryInit = state.startBlocks.get(state.blockId);
    if (primaryInit) {
      const pw = primaryInit.w;
      const ph = primaryInit.h;
      const targetX = primaryInit.x + deltaCanvasX;
      const targetY = primaryInit.y + deltaCanvasY;
      const snapThreshold = 5; // tolérance en pixels

      // Lignes de repères candidates (marges, axes médians et autres blocs)
      const candidateXLines: number[] = [
        this.safeMargeGauchePx,
        this.canvasWidth - this.safeMargeDroitePx,
        Math.round(this.canvasWidth / 2),
        Math.round((this.safeMargeGauchePx + this.canvasWidth - this.safeMargeDroitePx) / 2)
      ];

      const candidateYLines: number[] = [
        this.safeMargeHautPx + this.headerHeightPx,
        this.canvasHeight - this.safeMargeBasPx - this.footerHeightPx,
        Math.round((this.safeMargeHautPx + this.headerHeightPx + this.canvasHeight - this.safeMargeBasPx - this.footerHeightPx) / 2)
      ];

      for (const b of this.blocks) {
        if (state.startBlocks.has(b.id) || b.visible === false) continue;
        const fb = BLOCK_DEFAULT_DIMENSIONS[b.type];
        const bw = b.largeurBox || fb.w;
        const bh = b.hauteurBox || fb.h;
        const bx = b.x || 0;
        const by = b.y || 0;

        candidateXLines.push(bx, bx + Math.round(bw / 2), bx + bw);
        candidateYLines.push(by, by + Math.round(bh / 2), by + bh);
      }

      // Snapping horizontal (axe X)
      let snappedX: number | null = null;
      for (const cX of candidateXLines) {
        if (Math.abs(targetX - cX) <= snapThreshold) {
          snappedX = cX;
          this.activeVerticalGuides.push(cX);
          break;
        } else if (Math.abs(targetX + Math.round(pw / 2) - cX) <= snapThreshold) {
          snappedX = cX - Math.round(pw / 2);
          this.activeVerticalGuides.push(cX);
          break;
        } else if (Math.abs(targetX + pw - cX) <= snapThreshold) {
          snappedX = cX - pw;
          this.activeVerticalGuides.push(cX);
          break;
        }
      }
      if (snappedX !== null) {
        deltaCanvasX = snappedX - primaryInit.x;
      }

      // Snapping vertical (axe Y)
      let snappedY: number | null = null;
      for (const cY of candidateYLines) {
        if (Math.abs(targetY - cY) <= snapThreshold) {
          snappedY = cY;
          this.activeHorizontalGuides.push(cY);
          break;
        } else if (Math.abs(targetY + Math.round(ph / 2) - cY) <= snapThreshold) {
          snappedY = cY - Math.round(ph / 2);
          this.activeHorizontalGuides.push(cY);
          break;
        } else if (Math.abs(targetY + ph - cY) <= snapThreshold) {
          snappedY = cY - ph;
          this.activeHorizontalGuides.push(cY);
          break;
        }
      }
      if (snappedY !== null) {
        deltaCanvasY = snappedY - primaryInit.y;
      }
    }

    // Contrainte de groupe dans la zone utilisable
    const area = this.usableArea;
    let minAllowedDx = -Infinity;
    let maxAllowedDx = Infinity;
    let minAllowedDy = -Infinity;
    let maxAllowedDy = Infinity;

    state.startBlocks.forEach((init) => {
      minAllowedDx = Math.max(minAllowedDx, area.minX - init.x);
      maxAllowedDx = Math.min(maxAllowedDx, area.maxX - init.w - init.x);
      minAllowedDy = Math.max(minAllowedDy, area.minY - init.y);
      maxAllowedDy = Math.min(maxAllowedDy, area.maxY - init.h - init.y);
    });

    const effectiveDx = Math.max(minAllowedDx, Math.min(deltaCanvasX, maxAllowedDx));
    const effectiveDy = Math.max(minAllowedDy, Math.min(deltaCanvasY, maxAllowedDy));

    const nextBlocks = this.blocks.map(b => {
      const init = state.startBlocks.get(b.id);
      if (!init) return { ...b };
      return {
        ...b,
        x: Math.round(init.x + effectiveDx),
        y: Math.round(init.y + effectiveDy)
      };
    });

    if (!state.moved && (Math.abs(deltaScreenX) > 1 || Math.abs(deltaScreenY) > 1)) {
      state.moved = true;
    }

    this.blocks = nextBlocks;
    this.blocksPreviewChange.emit(nextBlocks);
    this.cdr.detectChanges();
  }

  @HostListener('window:mouseup')
  onWindowMouseUp(): void {
    this.activeVerticalGuides = [];
    this.activeHorizontalGuides = [];

    if (this.isMarquee) {
      this.isMarquee = false;
      this.marqueeRect = null;
      this.cdr.detectChanges();
      return;
    }

    if (!this.dragState) return;

    const state = this.dragState;
    // Si le bloc a réellement été déplacé, on contraint et on émet les changements
    if (state.moved) {
      const clampedBlocks = this.blocks.map(block => {
        const cloned = { ...block };
        this.clampBlockPosition(cloned);
        return cloned;
      });
      this.blocksChange.emit(clampedBlocks);
    }

    // Nettoyer l'état
    this.dragState = null;
    // Signaler qu'un drag vient de se terminer (pour ignorer le clic subséquent si on a bougé)
    this.wasDragging = state.moved;
    this.cdr.detectChanges();
  }

  // Surcharge de la sélection pour ignorer le clic après un drag
  selectBlock(block: DesignBlock, event?: MouseEvent): void {
    if (this.wasDragging) {
      // Ignorer le clic après un drag
      this.wasDragging = false;
      return;
    }

    if (this.locked) {
      this.lockedInteraction.emit();
      return;
    }

    // Si une touche modificatrice est active, la bascule a déjà été faite au mousedown
    if (event?.ctrlKey || event?.metaKey || event?.shiftKey) {
      return;
    }

    // Si le bloc n'était pas le seul sélectionné, on isole la sélection sur ce bloc
    if (this.selectedBlockIds.length > 1 || !this.selectedBlockIds.includes(block.id)) {
      this.setSelection([block.id], block);
    }
  }

  // ---------- Fin de la gestion manuelle ----------

  // Les méthodes ci-dessous restent inchangées par rapport à l'original
  // (replaceVariables, getVaraibleValue, clearSelection, setSelection, focusCanvas,
  // onKeyDown, moveSelectedBlocks, getStyle, getLigneStyle, getImageStyle,
  // getBoxStyle, getShapeStyle, displayContent, displayContent2, displayImageUrl,
  // isImageResolved, getRotationStyle)

  // Je les inclus pour assurer la complétude, mais elles sont identiques
  // à l'original fourni.

  replaceVariables(text: string): string {
    if (!text) return '';
    const values = this.fillerData?.getValues() || {};
    return text.replace(/\{\{(.+?)\}\}/g, (match, varName) => {
      const trimmed = varName.trim();
      return values[trimmed] !== undefined ? String(values[trimmed]) : match;
    });
  }

  getVaraibleValue(varName: string): string {
    const values = this.fillerData.getValues();
    return values[varName] !== undefined ? String(values[varName]) : `{{${varName}}}`;
  }

  clearSelection(): void {
    if (this.editingBlockId) {
      this.finishInPlaceEdit();
    }
    this.selectedBlockIds = [];
    this.blockSelected.emit(null as any);
    this.selectionChange.emit([]);
    this.cdr.detectChanges();
  }

  setSelection(ids: string[], lastSelected: DesignBlock): void {
    this.selectedBlockIds = [...new Set(ids)];
    this.blockSelected.emit(lastSelected);
    this.selectionChange.emit(this.selectedBlockIds);
    this.cdr.detectChanges();
  }

  focusCanvas(): void {
    if (this.editingBlockId) return;
    this.canvasContainer?.nativeElement?.focus();
  }

  onKeyDown(event: KeyboardEvent): void {
    if (this.locked) return;
    if (!this.selectedBlockIds.length) return;

    const step = event.shiftKey ? 10 : 1;
    let dx = 0;
    let dy = 0;

    switch (event.key) {
      case 'ArrowLeft': dx = -step; break;
      case 'ArrowRight': dx = step; break;
      case 'ArrowUp': dy = -step; break;
      case 'ArrowDown': dy = step; break;
      default: return;
    }

    event.preventDefault();
    this.moveSelectedBlocks(dx, dy);
  }

  moveSelectedBlocks(dx: number, dy: number): void {
    const selection = new Set(this.selectedBlockIds);
    const area = this.usableArea;
    for (const block of this.blocks) {
      if (!selection.has(block.id)) continue;
      const fallback = BLOCK_DEFAULT_DIMENSIONS[block.type];
      const w = block.largeurBox || fallback.w;
      const h = block.hauteurBox || fallback.h;
      // Position cible avant contrainte
      const targetX = (block.x || 0) + dx;
      const targetY = (block.y || 0) + dy;
      // Contrainte dans la zone utilisable
      block.x = Math.max(area.minX, Math.min(targetX, area.maxX - w));
      block.y = Math.max(area.minY, Math.min(targetY, area.maxY - h));
    }
    this.blocksChange.emit([...this.blocks]);
    this.cdr.detectChanges();
  }

  getStyle(block: DesignBlock): string {
    const s = block.style || {};
    let css = '';
    if (s.fontSize) css += `font-size:${s.fontSize}px;`;
    if (s.bold) css += 'font-weight:bold;';
    if (s.italic) css += 'font-style:italic;';
    if (s.underline) css += 'text-decoration:underline;';
    if (s.align) css += `text-align:${s.align};`;
    if (s.color) css += `color:${s.color};`;
    if (s.fontFamily) css += `font-family:${s.fontFamily};`;
    return css;
  }

  getLigneStyle(block: DesignBlock): string {
    const s = block.style || {};
    const epaisseur = s.epaisseur || 1;
    const couleur = s.couleur || '#000000';
    const largeur = s.largeur || 100;
    return `border-top:${epaisseur}px solid ${couleur};width:${largeur}%;`;
  }

  getImageStyle(block: DesignBlock): string {
    const s = block.style || {};
    const largeur = s.largeur || 100;
    const align = s.align || 'left';
    let css = `width:${largeur}px;`;
    if (align === 'center') css += 'display:block;margin:0 auto;';
    else if (align === 'right') css += 'display:block;margin-left:auto;';
    return css;
  }

  getBoxStyle(block: DesignBlock): { [key: string]: string } {
    const fallback = BLOCK_DEFAULT_DIMENSIONS[block.type];
    const w = block.largeurBox || fallback.w;
    const h = block.hauteurBox || fallback.h;
    return { width: `${w}px`, height: `${h}px` };
  }

  getShapeStyle(block: DesignBlock, isCircle = false): { [key: string]: string } {
    const largeur = block.largeurBox || (isCircle ? 100 : 150);
    const hauteur = block.hauteurBox || (isCircle ? largeur : 100);
    const fill = block.style?.fill || '#e5e7eb';
    const couleur = block.style?.couleur || '#94a3b8';
    const epaisseur = block.style?.epaisseur ?? 1;
    const radius = isCircle ? '50%' : `${block.style?.borderRadius ?? 0}px`;
    return {
      width: `${largeur}px`,
      height: `${hauteur}px`,
      background: fill,
      border: `${epaisseur}px solid ${couleur}`,
      'border-radius': radius,
      'box-sizing': 'border-box',
    };
  }

  displayContent(block: DesignBlock): string {
    return this.replaceVariables(block.contenu || '');
  }

  displayContent2(text: string): string {
    return this.replaceVariables(text || '');
  }

  displayImageUrl(block: DesignBlock): string {
    return this.replaceVariables(block.url || '');
  }

  isImageResolved(block: DesignBlock): boolean {
    const url = this.displayImageUrl(block);
    return !!url && !url.startsWith('{{');
  }

  getRotationStyle(block: DesignBlock): string {
    const parts: string[] = [];
    if (block.rotation) parts.push(`transform: rotate(${block.rotation}deg);`);
    if (block.opacite !== undefined && block.opacite !== 100) {
      parts.push(`opacity: ${block.opacite / 100};`);
    }
    return parts.join(' ');
  }

}