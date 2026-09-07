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

@Component({
  selector: 'app-design-canvas',
  standalone: true,
  imports: [CommonModule, LucideAngularModule], // DragDropModule supprimé
  templateUrl: './design-canvas.html',
  styleUrls: ['./design-canvas.scss'],
})
export class DesignCanvas implements OnInit, OnDestroy {
  @ViewChild('canvasContainer', { static: true }) canvasContainer?: ElementRef<HTMLDivElement>;

  @Input() blocks: DesignBlock[] = [];
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

  @Output() lockedInteraction = new EventEmitter<void>();
  @Output() blocksPreviewChange = new EventEmitter<DesignBlock[]>();
  @Output() blocksChange = new EventEmitter<DesignBlock[]>();
  @Output() blockSelected = new EventEmitter<DesignBlock>();

  private valuesSubscription?: Subscription;
  selectedBlockIds: string[] = [];

  // État du drag manuel
  private dragState: {
    blockId: string;
    startClientX: number;
    startClientY: number;
    startBlockX: number;
    startBlockY: number;
    moved: boolean;
  } | null = null;

  // Flag pour empêcher la sélection après un drag
  private wasDragging = false;

  get selectedBlockId(): string | null {
    return this.selectedBlockIds[this.selectedBlockIds.length - 1] ?? null;
  }

  // ---------- Zone utilisable (hors marges) ----------

  /** Retourne les bornes de la zone utilisable en pixels */
  get usableArea(): { minX: number; minY: number; maxX: number; maxY: number } {
    return {
      minX: this.margeGauchePx,
      minY: this.margeHautPx,
      maxX: this.canvasWidth - this.margeDroitePx,
      maxY: this.canvasHeight - this.margeBasPx,
    };
  }

  get contentAreaStyle(): { [key: string]: string } {
    const left = this.margeGauchePx;
    const top = this.margeHautPx;
    const width = this.canvasWidth - this.margeGauchePx - this.margeDroitePx;
    const height = this.canvasHeight - this.margeHautPx - this.margeBasPx;
    return {
      'left': `${left}px`,
      'top': `${top}px`,
      'width': `${width}px`,
      'height': `${height}px`,
    };
  }

  get marginTopStyle(): { [key: string]: string } {
    return {
      'left': '0',
      'top': '0',
      'width': `${this.canvasWidth}px`,
      'height': `${this.margeHautPx}px`,
    };
  }

  get marginBottomStyle(): { [key: string]: string } {
    return {
      'left': '0',
      'bottom': '0',
      'width': `${this.canvasWidth}px`,
      'height': `${this.margeBasPx}px`,
    };
  }

  get marginLeftStyle(): { [key: string]: string } {
    const topOffset = this.margeHautPx;
    const height = this.canvasHeight - this.margeHautPx - this.margeBasPx;
    return {
      'left': '0',
      'top': `${topOffset}px`,
      'width': `${this.margeGauchePx}px`,
      'height': `${Math.max(0, height)}px`,
    };
  }

  get marginRightStyle(): { [key: string]: string } {
    const topOffset = this.margeHautPx;
    const height = this.canvasHeight - this.margeHautPx - this.margeBasPx;
    return {
      'right': '0',
      'top': `${topOffset}px`,
      'width': `${this.margeDroitePx}px`,
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

    // Contraint la position : le bloc doit rester dans [minX, maxX] et [minY, maxY]
    const clampedX = Math.max(area.minX, Math.min(block.x || 0, area.maxX - effectiveW));
    const clampedY = Math.max(area.minY, Math.min(block.y || 0, area.maxY - effectiveH));

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

  /**
   * Déclenché au clic gauche sur un bloc (ou tout bouton si on ne filtre pas).
   * Initialise l'état du drag et capture les positions de départ.
   */
  onBlockMouseDown(event: MouseEvent, block: DesignBlock): void {
    // Ignorer si le canvas est verrouillé ou si le bloc est verrouillé
    if (this.locked || block.locked) {
      this.lockedInteraction.emit();
      return;
    }

    // Seul le clic gauche (bouton principal) est autorisé
    if (event.button !== 0) return;

    // Empêcher la sélection de texte, le défilement, etc.
    event.preventDefault();
    event.stopPropagation();

    // Initialiser l'état du drag
    this.dragState = {
      blockId: block.id,
      startClientX: event.clientX,
      startClientY: event.clientY,
      startBlockX: block.x || 0,
      startBlockY: block.y || 0,
      moved: false,
    };

    // Sélectionner le bloc si ce n'est pas déjà le cas
    if (!this.selectedBlockIds.includes(block.id)) {
      this.setSelection([block.id], block);
    }
  }

  @HostListener('window:mousemove', ['$event'])
  onWindowMouseMove(event: MouseEvent): void {
    if (!this.dragState) return;

    const state = this.dragState;
    const scale = this.zoom / 100 || 1;

    // Calcul du déplacement en pixels écran
    const deltaScreenX = event.clientX - state.startClientX;
    const deltaScreenY = event.clientY - state.startClientY;

    // Conversion en pixels canvas (division par le facteur de zoom)
    const deltaCanvasX = deltaScreenX / scale;
    const deltaCanvasY = deltaScreenY / scale;

    // Mettre à jour les coordonnées du bloc avec contrainte des marges
    const blockIndex = this.blocks.findIndex(b => b.id === state.blockId);
    if (blockIndex !== -1) {
      const area = this.usableArea;
      const nextBlocks = this.blocks.map((currentBlock, index) => {
        if (index !== blockIndex) return { ...currentBlock };

        const fallback = BLOCK_DEFAULT_DIMENSIONS[currentBlock.type];
        const w = currentBlock.largeurBox || fallback.w;
        const h = currentBlock.hauteurBox || fallback.h;

        // Position brute calculée à partir du delta
        let newX = state.startBlockX + deltaCanvasX;
        let newY = state.startBlockY + deltaCanvasY;

        // Contrainte : le bloc ne doit pas empiéter sur les marges
        newX = Math.max(area.minX, Math.min(newX, area.maxX - w));
        newY = Math.max(area.minY, Math.min(newY, area.maxY - h));

        return { ...currentBlock, x: newX, y: newY };
      });

      // Marquer comme déplacé si le déplacement dépasse un petit seuil
      if (!state.moved && (Math.abs(deltaScreenX) > 1 || Math.abs(deltaScreenY) > 1)) {
        state.moved = true;
      }

      this.blocksPreviewChange.emit(nextBlocks);
    }
  }

  @HostListener('window:mouseup')
  onWindowMouseUp(): void {
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
    // Signaler qu'un drag vient de se terminer (pour ignorer le prochain clic)
    this.wasDragging = true;
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

    if (event?.ctrlKey || event?.metaKey) {
      const alreadySelected = this.selectedBlockIds.includes(block.id);
      const ids = alreadySelected
        ? this.selectedBlockIds.filter(id => id !== block.id)
        : [...this.selectedBlockIds, block.id];
      this.setSelection(ids, block);
      return;
    }

    if (event?.shiftKey) {
      const ids = this.selectedBlockIds.includes(block.id)
        ? this.selectedBlockIds
        : [...this.selectedBlockIds, block.id];
      this.setSelection(ids, block);
      return;
    }

    this.setSelection([block.id], block);
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
    this.selectedBlockIds = [];
    this.blockSelected.emit(null as any);
    this.cdr.detectChanges();
  }

  setSelection(ids: string[], lastSelected: DesignBlock): void {
    this.selectedBlockIds = [...new Set(ids)];
    this.blockSelected.emit(lastSelected);
    this.focusCanvas();
    this.cdr.detectChanges();
  }

  focusCanvas(): void {
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