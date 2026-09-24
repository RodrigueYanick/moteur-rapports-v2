import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  LucideAngularModule, Undo2, Redo2, Grid3x3, Magnet, Ruler, Minus, Plus, Eye,
  Download, Rocket, Share2, Save, FileText, Check, Loader2,
  Edit, Copy, Settings, Trash2, AlignLeft, AlignCenter, AlignRight,
  ArrowUp, ArrowDown, Layers, MoveHorizontal, MoveVertical
} from 'lucide-angular';

@Component({
  selector: 'app-designer-toolbar',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './designer-toolbar.component.html',
  styleUrl: './designer-toolbar.component.scss'
})
export class DesignerToolbarComponent {
  @Input() templateName = '';
  @Input() templateStatus: 'BROUILLON' | 'PUBLIE' | 'ARCHIVE' = 'BROUILLON';
  @Input() savingStatus: 'idle' | 'saving' | 'saved' = 'idle';
  @Input() canCopy = false;
  @Input() canPaste = false;
  @Input() selectedBlockIds: string[] = [];
  @Input() showGrid = false;
  @Input() snapEnabled = false;
  @Input() showGuides = false;
  @Input() zoomPercent = 100;
  @Input() formatPapier = 'A4';
  @Input() effectiveMargeHautMm = 10;
  @Input() isLocked = false;
  @Input() fillingMode = false;

  @Output() undo = new EventEmitter<void>();
  @Output() redo = new EventEmitter<void>();
  @Output() copyBlock = new EventEmitter<void>();
  @Output() pasteBlock = new EventEmitter<void>();
  @Output() duplicateBlock = new EventEmitter<void>();
  @Output() deleteBlock = new EventEmitter<void>();
  @Output() alignBlocks = new EventEmitter<'left' | 'centerH' | 'right' | 'top' | 'bottom'>();
  @Output() distributeBlocks = new EventEmitter<'horizontal' | 'vertical'>();
  @Output() toggleGrid = new EventEmitter<void>();
  @Output() toggleSnap = new EventEmitter<void>();
  @Output() toggleGuides = new EventEmitter<void>();
  @Output() zoomIn = new EventEmitter<void>();
  @Output() zoomOut = new EventEmitter<void>();
  @Output() openSettings = new EventEmitter<void>();
  @Output() requestDuplicate = new EventEmitter<void>();
  @Output() toggleFilling = new EventEmitter<void>();
  @Output() startTour = new EventEmitter<void>();
  @Output() preview = new EventEmitter<void>();
  @Output() exportPdf = new EventEmitter<void>();
  @Output() exportHtml = new EventEmitter<void>();
  @Output() save = new EventEmitter<void>();
  @Output() publish = new EventEmitter<void>();

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
    edit: Edit,
    copy: Copy,
    fileText: FileText,
    settings: Settings,
    trash: Trash2,
    alignLeft: AlignLeft,
    alignCenter: AlignCenter,
    alignRight: AlignRight,
    alignTop: ArrowUp,
    alignBottom: ArrowDown,
    layers: Layers,
    distributeH: MoveHorizontal,
    distributeV: MoveVertical,
  };
}

