import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnDestroy,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { CdkDragDrop, CdkDragEnd, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { FillerDataService } from '../services/filler-data';
import { Subscription } from 'rxjs';
import { LucideAngularModule, ImageOff, Square, QrCode, Barcode, PenTool } from 'lucide-angular';
import { DesignBlock, BLOCK_DEFAULT_DIMENSIONS } from '../models/design-block.model';

@Component({
  selector: 'app-design-canvas',
  standalone: true,
  imports: [CommonModule, DragDropModule, LucideAngularModule],
  templateUrl: './design-canvas.html',
  styleUrls: ['./design-canvas.scss'],
})
export class DesignCanvas implements OnInit, OnDestroy {
  @Input() blocks: DesignBlock[] = [];
  @Input() zoom: number = 100;
  @Input() canvasHeight: number = 1123;
  @Input() canvasWidth: number = 794;
  @Input() showGrid = false;
  @Input() snapEnabled = false;
  @Input() showGuides = false;
  @Input() locked = false;

  @Output() lockedInteraction = new EventEmitter<void>();
  @Output() blocksChange = new EventEmitter<DesignBlock[]>();
  @Output() blockSelected = new EventEmitter<DesignBlock>();

  private valuesSubscription?: Subscription;
  selectedBlockId: string | null = null;

  readonly icons = {
    imagePlaceholder: ImageOff,
    qrcode: QrCode,
    barcode: Barcode,
    penTool: PenTool,
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
  }

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

  selectBlock(block: DesignBlock): void {
    if (this.locked) {
      this.lockedInteraction.emit();
      return;
    }
    this.selectedBlockId = block.id;
    this.blockSelected.emit(block);
  }

  onDrop(event: CdkDragDrop<DesignBlock[]>): void {
    if (event.previousIndex === event.currentIndex) return;
    moveItemInArray(this.blocks, event.previousIndex, event.currentIndex);
    this.blocksChange.emit([...this.blocks]);
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

  // Calcule la taille du conteneur du bloc à partir de largeurBox/hauteurBox,
  // avec un fallback cohérent selon le type si l'utilisateur n'a rien défini.
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

  onDragEnded(block: DesignBlock, event: CdkDragEnd): void {
    const element = event.source.element.nativeElement;
    const transform = element.style.transform;
    const match = transform.match(/translate3d\((.+)px, (.+)px, 0px\)/);
    if (match) {
      const scaleFactor = this.zoom / 100;
      const deltaX = parseFloat(match[1]) / scaleFactor;
      const deltaY = parseFloat(match[2]) / scaleFactor;

      let newX = (block.x || 0) + deltaX;
      let newY = (block.y || 0) + deltaY;

      // Empêche toute position négative (bloc éjecté hors de la page)
      newX = Math.max(0, newX);
      newY = Math.max(0, newY);

      block.x = newX;
      block.y = newY;
      element.style.transform = '';
      this.blocksChange.emit([...this.blocks]);
    }
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
