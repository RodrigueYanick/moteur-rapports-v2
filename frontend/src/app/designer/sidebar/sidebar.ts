import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DesignBlock } from '../models/design-block.model';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import {
  LucideAngularModule,
  LibraryBig,
  Layers,
  Type,
  AlignLeft,
  Table2,
  Minus,
  Image,
  Square,
  Circle,
  QrCode,
  Barcode,
  PenTool,
  BarChart3,
  Eye,
  EyeOff,
  Lock,
  Unlock,
  ChevronDown,
  ChevronRight,
  ChevronUp,
  Code,
  GripVertical,
  ChevronsUp,
  ChevronsDown,
} from 'lucide-angular';
import { VariableManager } from '../variable-manager/variable-manager';
import { Variable } from '../../models/variable.model';

interface ComponentDef {
  type: DesignBlock['type'];
  label: string;
  icon: any;
}

interface ComponentCategory {
  id: string;
  label: string;
  items: ComponentDef[];
  open: boolean;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, LucideAngularModule, VariableManager, DragDropModule],
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.scss'],
})
export class Sidebar {
  @Input() blocks: DesignBlock[] = [];
  @Input() selectedBlock: DesignBlock | null = null;
  @Input() templateId: string | null = null;
  @Input() locked = false;

  @Output() lockedInteraction = new EventEmitter<void>();
  @Output() blockSelected = new EventEmitter<DesignBlock>();
  @Output() toggleVisibility = new EventEmitter<DesignBlock>();
  @Output() toggleLock = new EventEmitter<DesignBlock>();
  @Output() addBlock = new EventEmitter<DesignBlock['type']>();
  @Output() explicitVariablesChange = new EventEmitter<Variable[]>();
  @Output() insertVariable = new EventEmitter<Variable>();
  /** Émet le tableau de blocs réordonné (dans l'ordre réel du canvas, pas l'ordre affiché inversé) */
  @Output() blocksReordered = new EventEmitter<DesignBlock[]>();

  activeTab: 'library' | 'layers' | 'variables' = 'layers';

  readonly icons = {
    library: LibraryBig,
    layers: Layers,
    eye: Eye,
    eyeOff: EyeOff,
    lock: Lock,
    unlock: Unlock,
    chevronDown: ChevronDown,
    chevronRight: ChevronRight,
    chevronUp: ChevronUp,
    code: Code,
    grip: GripVertical,
    toFront: ChevronsUp,
    toBack: ChevronsDown,
  };

  private typeIcons: Record<DesignBlock['type'], any> = {
    titre: Type,
    texte: AlignLeft,
    tableau: Table2,
    ligne: Minus,
    image: Image,
    rectangle: Square,
    cercle: Circle,
    qrcode: QrCode,
    codebarre: Barcode,
    signature: PenTool,
    graphique: BarChart3,
  };

  categories: ComponentCategory[] = [
    {
      id: 'contenu',
      label: 'Contenu',
      open: true,
      items: [
        { type: 'titre', label: 'Titre', icon: this.typeIcons['titre'] },
        { type: 'texte', label: 'Texte', icon: this.typeIcons['texte'] },
      ],
    },
    {
      id: 'medias',
      label: 'Médias',
      open: true,
      items: [
        { type: 'image', label: 'Image', icon: this.typeIcons['image'] },
        { type: 'signature', label: 'Signature', icon: this.typeIcons['signature'] },
      ],
    },
    {
      id: 'formes',
      label: 'Formes',
      open: true,
      items: [
        { type: 'ligne', label: 'Ligne', icon: this.typeIcons['ligne'] },
        { type: 'rectangle', label: 'Rectangle', icon: this.typeIcons['rectangle'] },
        { type: 'cercle', label: 'Cercle', icon: this.typeIcons['cercle'] },
      ],
    },
    {
      id: 'donnees',
      label: 'Données',
      open: true,
      items: [
        { type: 'tableau', label: 'Tableau', icon: this.typeIcons['tableau'] },
        { type: 'graphique', label: 'Graphique', icon: this.typeIcons['graphique'] },
      ],
    },
    {
      id: 'codes',
      label: 'Codes',
      open: false,
      items: [
        { type: 'qrcode', label: 'QR Code', icon: this.typeIcons['qrcode'] },
        { type: 'codebarre', label: 'Code-barres', icon: this.typeIcons['codebarre'] },
      ],
    },
  ];

  recentlyAddedType: DesignBlock['type'] | null = null;

  setActiveTab(tab: 'library' | 'layers' | 'variables'): void {
    this.activeTab = tab;
  }

  toggleCategory(cat: ComponentCategory): void {
    setTimeout(() => (cat.open = !cat.open));
  }

  onAddBlock(type: DesignBlock['type']): void {
    if (this.locked) {
      this.lockedInteraction.emit();
      return;
    }
    this.addBlock.emit(type);
    this.recentlyAddedType = type;
    setTimeout(() => {
      if (this.recentlyAddedType === type) {
        this.recentlyAddedType = null;
      }
    }, 500);
  }

  onSelectBlock(block: DesignBlock): void {
    this.blockSelected.emit(block);
  }

  onToggleVisibility(block: DesignBlock): void {
    this.toggleVisibility.emit(block);
  }

  onToggleLock(block: DesignBlock): void {
    this.toggleLock.emit(block);
  }

  blockIcon(type: DesignBlock['type']): any {
    return this.typeIcons[type] || this.typeIcons['texte'];
  }

  // ============================================================
  // CALQUES — gestion de l'ordre d'empilement (comme Figma)
  // ============================================================
  //
  // `blocks` est la source de vérité (ordre réel du canvas) :
  // le DERNIER élément du tableau = premier plan (rendu en dernier dans le DOM).
  //
  // Dans le panneau, on affiche l'INVERSE de `blocks` pour respecter la
  // convention Figma : le calque du HAUT de la liste = premier plan.
  //
  // On ne mute jamais `blocks` directement ici : toute réorganisation
  // recalcule un nouveau tableau dans l'ordre réel, puis l'émet au parent.

  get layersDisplayOrder(): DesignBlock[] {
    return [...this.blocks].reverse();
  }

  trackByBlockId(index: number, block: DesignBlock): string {
    return block.id;
  }

  onLayerDrop(event: CdkDragDrop<DesignBlock[]>): void {
    if (this.locked) {
      this.lockedInteraction.emit();
      return;
    }
    if (event.previousIndex === event.currentIndex) return;

    // Réordonne une copie de la liste affichée (inversée)
    const displayOrder = this.layersDisplayOrder;
    moveItemInArray(displayOrder, event.previousIndex, event.currentIndex);

    // Reconvertit vers l'ordre réel du canvas avant d'émettre
    const realOrder = [...displayOrder].reverse();
    this.blocksReordered.emit(realOrder);
  }

  /** Fait remonter un calque d'un cran (vers le premier plan) */
  moveLayerUp(block: DesignBlock, event: Event): void {
    event.stopPropagation();
    if (this.locked) { this.lockedInteraction.emit(); return; }
    const idx = this.blocks.findIndex(b => b.id === block.id);
    if (idx === -1 || idx === this.blocks.length - 1) return;
    const reordered = [...this.blocks];
    [reordered[idx], reordered[idx + 1]] = [reordered[idx + 1], reordered[idx]];
    this.blocksReordered.emit(reordered);
  }

  /** Fait descendre un calque d'un cran (vers l'arrière-plan) */
  moveLayerDown(block: DesignBlock, event: Event): void {
    event.stopPropagation();
    if (this.locked) { this.lockedInteraction.emit(); return; }
    const idx = this.blocks.findIndex(b => b.id === block.id);
    if (idx <= 0) return;
    const reordered = [...this.blocks];
    [reordered[idx], reordered[idx - 1]] = [reordered[idx - 1], reordered[idx]];
    this.blocksReordered.emit(reordered);
  }

  /** Envoie un calque tout au premier plan */
  bringToFront(block: DesignBlock, event: Event): void {
    event.stopPropagation();
    if (this.locked) { this.lockedInteraction.emit(); return; }
    const idx = this.blocks.findIndex(b => b.id === block.id);
    if (idx === -1 || idx === this.blocks.length - 1) return;
    const reordered = this.blocks.filter(b => b.id !== block.id);
    reordered.push(block);
    this.blocksReordered.emit(reordered);
  }

  /** Envoie un calque tout à l'arrière-plan */
  sendToBack(block: DesignBlock, event: Event): void {
    event.stopPropagation();
    if (this.locked) { this.lockedInteraction.emit(); return; }
    const idx = this.blocks.findIndex(b => b.id === block.id);
    if (idx <= 0) return;
    const reordered = this.blocks.filter(b => b.id !== block.id);
    reordered.unshift(block);
    this.blocksReordered.emit(reordered);
  }

  isFirstInDisplay(block: DesignBlock): boolean {
    return this.layersDisplayOrder[0]?.id === block.id;
  }

  isLastInDisplay(block: DesignBlock): boolean {
    const arr = this.layersDisplayOrder;
    return arr[arr.length - 1]?.id === block.id;
  }
}