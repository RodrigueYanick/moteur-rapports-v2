import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DesignBlock } from '../models/design-block.model';
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
  Code
} from 'lucide-angular';
import { VariableManager } from '../variable-manager/variable-manager';
import { Template } from '../../models/template.model';
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
  imports: [CommonModule, LucideAngularModule, VariableManager],
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.scss']
})
export class Sidebar {
  @Input() blocks: DesignBlock[] = [];
  @Input() selectedBlock: DesignBlock | null = null;
  @Input() templateId: string | null = null;
  @Output() blockSelected = new EventEmitter<DesignBlock>();
  @Output() toggleVisibility = new EventEmitter<DesignBlock>();
  @Output() toggleLock = new EventEmitter<DesignBlock>();
  @Output() addBlock = new EventEmitter<DesignBlock['type']>();
  @Output() explicitVariablesChange = new EventEmitter<Variable[]>();

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
    code: Code
  };

  // Icônes par type de bloc, utilisées aussi dans l'onglet Calques
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
    graphique: BarChart3
  };

  // Bibliothèque organisée en catégories repliables
  categories: ComponentCategory[] = [
    {
      id: 'contenu',
      label: 'Contenu',
      open: true,
      items: [
        { type: 'titre', label: 'Titre', icon: this.typeIcons['titre'] },
        { type: 'texte', label: 'Texte', icon: this.typeIcons['texte'] },
      ]
    },
    {
      id: 'medias',
      label: 'Médias',
      open: true,
      items: [
        { type: 'image', label: 'Image', icon: this.typeIcons['image'] },
        { type: 'signature', label: 'Signature', icon: this.typeIcons['signature'] },
      ]
    },
    {
      id: 'formes',
      label: 'Formes',
      open: true,
      items: [
        { type: 'ligne', label: 'Ligne', icon: this.typeIcons['ligne'] },
        { type: 'rectangle', label: 'Rectangle', icon: this.typeIcons['rectangle'] },
        { type: 'cercle', label: 'Cercle', icon: this.typeIcons['cercle'] },
      ]
    },
    {
      id: 'donnees',
      label: 'Données',
      open: true,
      items: [
        { type: 'tableau', label: 'Tableau', icon: this.typeIcons['tableau'] },
        { type: 'graphique', label: 'Graphique', icon: this.typeIcons['graphique'] },
      ]
    },
    {
      id: 'codes',
      label: 'Codes',
      open: false,
      items: [
        { type: 'qrcode', label: 'QR Code', icon: this.typeIcons['qrcode'] },
        { type: 'codebarre', label: 'Code-barres', icon: this.typeIcons['codebarre'] },
      ]
    }
  ];

  // Type mis en avant brièvement après un clic (retour visuel "sélection")
  recentlyAddedType: DesignBlock['type'] | null = null;

  setActiveTab(tab: 'library' | 'layers' | 'variables'): void {
    this.activeTab = tab;
  }

  toggleCategory(cat: ComponentCategory): void {
    cat.open = !cat.open;
  }

  onAddBlock(type: DesignBlock['type']): void {
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
}