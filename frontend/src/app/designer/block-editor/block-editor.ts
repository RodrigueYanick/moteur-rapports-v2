import { Component, Input, Output, EventEmitter, OnChanges, DoCheck, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  FormArray,
  Validators,
  FormsModule,
  FormControl,
} from '@angular/forms';
import { DesignBlock, TableCell, BLOCK_DEFAULT_DIMENSIONS } from '../models/design-block.model';
import { debounceTime, Subject } from 'rxjs';
import { Variable } from '../../models/variable.model'; // NOUVEAU
import {
  LucideAngularModule,
  X,
  Plus,
  Settings2,
  AlignLeft,
  AlignCenter,
  AlignRight,
  AlignStartVertical,
  AlignCenterVertical,
  AlignEndVertical,
  Bold,
  Italic,
  Underline,
  Eye,
  EyeOff,
  Lock,
  Unlock,
  Database,
  ChevronDown,
  ChevronUp,
  PaintBucket,
  Palette,
  ChevronsRight,
  ChevronsDown,
} from 'lucide-angular';

@Component({
  selector: 'app-block-editor',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, LucideAngularModule],
  templateUrl: './block-editor.html',
  styleUrls: ['./block-editor.scss'],
})
export class BlockEditor implements OnChanges, DoCheck {
  @Input() block: DesignBlock | null = null;
  @Input() allBlocks: DesignBlock[] = [];
  @Input() explicitVariables: Variable[] = []; // MODIFIÉ : type Variable[] au lieu de { nomVariable: string }[]
  @Output() updated = new EventEmitter<DesignBlock>();
  @Output() closed = new EventEmitter<void>();
  newVariableName = '';
  selectedColumn: number | null = null;
  selectedRow: number | null = null;
  selectedCell: { row: number; col: number } | null = null;

  readonly icons = {
    close: X,
    add: Plus,
    settings: Settings2,
    alignLeft: AlignLeft,
    alignCenter: AlignCenter,
    alignRight: AlignRight,
    alignTop: AlignStartVertical,
    alignMiddle: AlignCenterVertical,
    alignBottom: AlignEndVertical,
    bold: Bold,
    italic: Italic,
    underline: Underline,
    eye: Eye,
    eyeOff: EyeOff,
    lock: Lock,
    unlock: Unlock,
    data: Database,
    chevronDown: ChevronDown,
    chevronUp: ChevronUp,
    fill: PaintBucket,
    palette: Palette,
    mergeRight: ChevronsRight,
    mergeDown: ChevronsDown,
  };

  form: FormGroup;
  availableVariables: string[] = [];
  private tableChange$ = new Subject<void>();

  debugNgChanges = 0;
  debugValueChanges = 0;

  tableRows = 2;
  tableCols = 2;

  sectionsOpen: Record<string, boolean> = {
    general: true,
    position: true,
    dimensions: true,
    apparence: true,
    alignement: true,
    typographie: true,
    donnees: true,
    tableauStyle: true,
  };

  fontFamilies = ['Inter', 'Arial', 'Georgia', 'Times New Roman', 'Courier New', 'Roboto'];

  constructor(private fb: FormBuilder) {
    this.form = this.fb.group({
      nom: [''],
      contenu: [''],
      fontSize: [12],
      bold: [false],
      italic: [false],
      underline: [false],
      align: ['left'],
      verticalAlign: ['top'],
      color: ['#000000'],
      fontFamily: ['Inter'],
      posX: [0],
      posY: [0],
      largeurBox: [200],
      hauteurBox: [50],
      opacite: [100],
      visible: [true],
      locked: [false],
      epaisseur: [1],
      couleurLigne: ['#000000'],
      largeurLigne: [100],
      url: [''],
      largeurImage: [100],
      alignImage: ['left'],
      source: [''],
      colonnes: this.fb.array([]),
      dataFormat: [''],
      dataDefault: [''],
      bordureCouleur: ['#d9d9d9'],
      texteCouleurDefaut: ['#000000'],
      rotation: [0],
      shapeFill: ['#e5e7eb'],
      shapeBorderColor: ['#94a3b8'],
      shapeBorderWidth: [1],
      shapeBorderRadius: [0],
    });

    this.tableChange$.pipe(debounceTime(300)).subscribe(() => {
      if (this.block) this.updated.emit(this.block);
    });

    this.form.valueChanges.subscribe(() => {
      this.debugValueChanges++;
    });
  }

  // ==================== NOUVEAU : variables ARRAY disponibles ====================
  get arrayVariables(): Variable[] {
    return (this.explicitVariables || []).filter(v => v.type === 'ARRAY');
  }

  // ==================== NOUVEAU : bascule Statique / Dynamique ====================
  switchToStatic(): void {
    if (!this.block || this.block.type !== 'tableau' || this.block.lignes) return;
    const newLignes: TableCell[][] = [];
    for (let i = 0; i < this.tableRows; i++) {
      const row: TableCell[] = [];
      for (let j = 0; j < this.tableCols; j++) row.push({ value: '' });
      newLignes.push(row);
    }
    this.block.lignes = newLignes;
    this.block.source = undefined;
    this.block.colonnes = undefined;
    this.selectedCell = null;
    this.selectedColumn = null;
    this.selectedRow = null;
    this.tableChange$.next();
  }

  switchToDynamic(): void {
    if (!this.block || this.block.type !== 'tableau' || !this.block.lignes) return;
    this.block.lignes = undefined;
    this.block.source = this.block.source || '';
    this.block.colonnes = this.block.colonnes?.length
      ? this.block.colonnes
      : [{ titre: '', variable: '' }];
    this.form.patchValue({ source: this.block.source }, { emitEvent: false });
    this.buildColonnesArray(this.block.colonnes);
    this.selectedCell = null;
    this.selectedColumn = null;
    this.selectedRow = null;
    this.tableChange$.next();
  }

  onSourceChange(value: string): void {
    if (!this.block) return;
    this.block.source = value;
    this.tableChange$.next();
  }

  // ==================== FIN NOUVEAU ====================

  toggleSection(key: string): void {
    this.sectionsOpen[key] = !this.sectionsOpen[key];
  }

  updateTableDimensions(): void {
    if (!this.block || this.block.type !== 'tableau') return;
    const newLignes: TableCell[][] = [];
    for (let i = 0; i < this.tableRows; i++) {
      const row: TableCell[] = [];
      for (let j = 0; j < this.tableCols; j++) {
        row.push({ value: '' });
      }
      newLignes.push(row);
    }
    this.block.lignes = newLignes;
    this.selectedCell = null;
    this.selectedColumn = null;
    this.selectedRow = null;
    this.tableChange$.next();
  }

  updateCellValue(row: number, col: number, value: string): void {
    if (!this.block?.lignes || !this.block.lignes[row] || !this.block.lignes[row][col]) return;
    
    // ✅ CORRECTION: Mutation in-place au lieu de copie profonde complète
    // Cela évite que le DOM soit recréé à chaque caractère saisi
    this.block.lignes[row][col].value = value;
    
    // Trigger une mise à jour sans changer la référence du tableau entier
    this.tableChange$.next();
  }

  selectCell(row: number, col: number): void {
    this.selectedCell = { row, col };
    this.selectedColumn = null;
    this.selectedRow = null;
  }

  isCellSelected(row: number, col: number): boolean {
    return this.selectedCell?.row === row && this.selectedCell?.col === col;
  }

  get selectedCellData(): TableCell | null {
    if (!this.selectedCell || !this.block?.lignes) return null;
    return this.block.lignes[this.selectedCell.row]?.[this.selectedCell.col] || null;
  }

  selectColumn(colIndex: number): void {
    this.selectedColumn = colIndex;
    this.selectedCell = null;
    this.selectedRow = null;
  }

  selectRow(rowIndex: number): void {
    this.selectedRow = rowIndex;
    this.selectedCell = null;
    this.selectedColumn = null;
  }

  updateColumnStyle(field: 'bgColor' | 'textColor', value: string): void {
    if (this.selectedColumn === null || !this.block?.lignes) return;
    const col = this.selectedColumn;
    const newLignes = this.block.lignes.map((row) =>
      row.map((cell, idx) => (idx === col ? { ...cell, [field]: value } : { ...cell })),
    );
    this.block.lignes = newLignes;
    this.tableChange$.next();
  }

  updateSelectedCellStyle(field: 'bgColor' | 'textColor', value: string): void {
    if (!this.selectedCell || !this.block?.lignes) return;
    const { row, col } = this.selectedCell;
    const newLignes = this.block.lignes.map((r) => r.map((c) => ({ ...c })));
    newLignes[row][col] = { ...newLignes[row][col], [field]: value };
    this.block.lignes = newLignes;
    this.tableChange$.next();
  }

  updateRowStyle(field: 'bgColor' | 'textColor', value: string): void {
    if (this.selectedRow === null || !this.block?.lignes) return;
    const row = this.selectedRow;
    const newLignes = this.block.lignes.map((r, idx) =>
      idx === row ? r.map(c => ({ ...c, [field]: value })) : r.map(c => ({ ...c }))
    );
    this.block.lignes = newLignes;
    this.tableChange$.next();
  }

  insertVariableInCell(varName: string): void {
    if (!this.selectedCell || !this.block?.lignes) return;
    const { row, col } = this.selectedCell;
    const current = this.block.lignes[row][col].value || '';
    this.updateCellValue(row, col, current + `{{${varName}}}`);
  }

  trackByIndex(index: number): number {
    return index;
  }

  ngOnChanges(changes: SimpleChanges): void {
    this.debugNgChanges++;
    this.refreshAvailableVariables();

    if (changes['block'] && this.block) {
      const fallback = BLOCK_DEFAULT_DIMENSIONS[this.block.type];
      this.form.patchValue(
        {
          nom: this.block.nom || this.defaultName(),
          contenu: this.block.contenu || '',
          fontSize: this.block.style?.fontSize || 12,
          bold: this.block.style?.bold || false,
          italic: this.block.style?.italic || false,
          underline: this.block.style?.underline || false,
          align: this.block.style?.align || 'left',
          verticalAlign: this.block.style?.verticalAlign || 'top',
          color: this.block.style?.color || '#000000',
          fontFamily: this.block.style?.fontFamily || 'Inter',
          posX: this.block.x || 0,
          posY: this.block.y || 0,
          largeurBox: this.block.largeurBox || fallback.w,
          hauteurBox: this.block.hauteurBox || fallback.h,
          opacite: this.block.opacite ?? 100,
          visible: this.block.visible !== false,
          locked: this.block.locked || false,
          epaisseur: this.block.style?.epaisseur || 1,
          couleurLigne: this.block.style?.couleur || '#000000',
          largeurLigne: this.block.style?.largeur || 100,
          url: this.block.url || '',
          largeurImage: this.block.style?.largeur || 100,
          alignImage: this.block.style?.align || 'left',
          source: this.block.source || '',
          dataFormat: this.block.dataBinding?.format || '',
          dataDefault: this.block.dataBinding?.valeurDefaut || '',
          bordureCouleur: this.block.style?.bordureCouleur || '#d9d9d9',
          texteCouleurDefaut: this.block.style?.texteCouleurDefaut || '#000000',
          rotation: this.block.rotation ?? 0,
          shapeFill: this.block.style?.fill || '#e5e7eb',
          shapeBorderColor: this.block.style?.couleur || '#94a3b8',
          shapeBorderWidth: this.block.style?.epaisseur ?? 1,
          shapeBorderRadius: this.block.style?.borderRadius ?? 0,
        },
        { emitEvent: false },
      );
      this.buildColonnesArray(this.block.colonnes || []);
      this.selectedRow = null;
    }

    if (changes['block']) {
      const prevId = changes['block'].previousValue?.id as string | undefined;
      const newId = this.block?.id as string | undefined;
      if (!prevId || prevId !== newId) {
        this.selectedCell = null;
      }
    }

    if (this.block && this.block.type === 'tableau' && this.block.lignes) {
      this.tableRows = this.block.lignes.length;
      this.tableCols = this.block.lignes[0]?.length || 2;
    }
  }

  ngDoCheck(): void {
    if (!this.block) return;
    const posXControl = this.form.get('posX');
    const posYControl = this.form.get('posY');
    if (!posXControl || !posYControl) return;

    const blockX = this.block.x ?? 0;
    const blockY = this.block.y ?? 0;
    const formX = Number(posXControl.value) ?? 0;
    const formY = Number(posYControl.value) ?? 0;

    if (blockX !== formX && !posXControl.dirty && !posXControl.touched) {
      posXControl.setValue(blockX, { emitEvent: false });
    }
    if (blockY !== formY && !posYControl.dirty && !posYControl.touched) {
      posYControl.setValue(blockY, { emitEvent: false });
    }
  }

  private defaultName(): string {
    if (!this.block) return '';
    return `${this.block.type.charAt(0).toUpperCase()}${this.block.type.slice(1)}`;
  }

  get colonnes(): FormArray {
    return this.form.get('colonnes') as FormArray;
  }

  buildColonnesArray(cols: { titre: string; variable: string; }[]): void {
    while (this.colonnes.length) this.colonnes.removeAt(0);
    cols.forEach((col) => this.addColonne(col.titre, col.variable));
  }

  addColonne(titre = '', variable = ''): void {
    this.colonnes.push(
      this.fb.group({
        titre: [titre, Validators.required],
        variable: [variable, Validators.required],
      }),
    );
  }

  removeColonne(index: number): void {
    this.colonnes.removeAt(index);
  }

  refreshAvailableVariables(): void {
    const explicitNames = this.explicitVariables?.map((v) => v.nomVariable) || [];
    const detected = new Set<string>(explicitNames);

    for (const b of this.allBlocks) {
      const texte = b.contenu || b.url || '';
      const matches = texte.match(/\{\{(.+?)\}\}/g);
      if (matches)
        matches.forEach((m) => detected.add(m.replace('{{', '').replace('}}', '').trim()));

      if (b.lignes) {
        for (const row of b.lignes) {
          for (const cell of row) {
            const cellMatches = (cell.value || '').match(/\{\{(.+?)\}\}/g);
            if (cellMatches)
              cellMatches.forEach((m) =>
                detected.add(m.replace('{{', '').replace('}}', '').trim()),
              );
          }
        }
      }
    }
    this.availableVariables = Array.from(detected).sort();
  }

  insertVariable(varName: string): void {
    const ctrl = this.form.get('contenu');
    if (ctrl) ctrl.setValue((ctrl.value || '') + `{{${varName}}}`);
  }

  insertVariableIntoUrl(varName: string): void {
    const ctrl = this.form.get('url');
    if (ctrl) ctrl.setValue((ctrl.value || '') + `{{${varName}}}`);
  }

  get detectedVariable(): string | null {
    if (!this.block) return null;
    const texte = this.block.type === 'image' ? this.block.url || '' : this.block.contenu || '';
    const match = texte.match(/\{\{(.+?)\}\}/);
    if (match) return match[1].trim();
    if (this.block.type === 'tableau') return this.block.source || null;
    return null;
  }

  get isDynamic(): boolean {
    return !!this.detectedVariable;
  }

  setAlign(value: 'left' | 'center' | 'right'): void {
    this.form.get('align')?.setValue(value);
  }

  setVerticalAlign(value: 'top' | 'middle' | 'bottom'): void {
    this.form.get('verticalAlign')?.setValue(value);
  }

  setImageAlign(value: 'left' | 'center' | 'right'): void {
    this.form.get('alignImage')?.setValue(value);
  }

  get validationMessages(): string[] {
    const messages: string[] = [];

    if (this.block?.type === 'tableau') {
      const controls = this.colonnes.controls;
      if (controls.some((col) => col.get('titre')?.invalid)) {
        messages.push('Chaque colonne doit avoir un titre.');
      }
      if (controls.some((col) => col.get('variable')?.invalid)) {
        messages.push('Chaque colonne doit avoir une variable.');
      }
    }

    if (this.form.invalid && messages.length === 0) {
      messages.push('Le formulaire contient des champs invalides.');
    }

    return messages;
  }

  save(): void {
    if (!this.block || this.form.invalid) return;
    const val = this.form.value;
    let updatedBlock: DesignBlock = {
      ...this.block,
      nom: val.nom,
      x: val.posX,
      y: val.posY,
      largeurBox: val.largeurBox,
      hauteurBox: val.hauteurBox,
      opacite: val.opacite,
      visible: val.visible,
      locked: val.locked,
      rotation: val.rotation,
      colonnes: val.colonnes || [],
      dataBinding: { format: val.dataFormat, valeurDefaut: val.dataDefault },
    };

    switch (this.block.type) {
      case 'titre':
      case 'texte':
        updatedBlock.contenu = val.contenu;
        updatedBlock.style = {
          ...updatedBlock.style,
          fontSize: val.fontSize,
          bold: val.bold,
          italic: val.italic,
          underline: val.underline,
          align: val.align,
          verticalAlign: val.verticalAlign,
          color: val.color,
          fontFamily: val.fontFamily,
        };
        break;
      case 'ligne':
        updatedBlock.style = {
          ...updatedBlock.style,
          epaisseur: val.epaisseur,
          couleur: val.couleurLigne,
          largeur: val.largeurLigne,
        };
        break;
      case 'image':
        updatedBlock.url = val.url;
        updatedBlock.style = {
          ...updatedBlock.style,
          largeur: val.largeurImage,
          align: val.alignImage,
        };
        break;
      case 'tableau':
      if (this.block.lignes) {
        // Mode statique : on garde les lignes, on supprime les propriétés dynamiques
        updatedBlock.lignes = this.block.lignes;
        updatedBlock.source = undefined;
        updatedBlock.colonnes = undefined;
      } else {
        // Mode dynamique : on utilise la source et les colonnes
        updatedBlock.source = val.source;
        updatedBlock.colonnes = val.colonnes;
        updatedBlock.lignes = undefined;
      }
      updatedBlock.style = {
        ...updatedBlock.style,
        bordureCouleur: val.bordureCouleur,
        texteCouleurDefaut: val.texteCouleurDefaut,
      };
      break;
      case 'rectangle':
      case 'cercle':
        updatedBlock.style = {
          ...updatedBlock.style,
          fill: val.shapeFill,
          couleur: val.shapeBorderColor,
          epaisseur: val.shapeBorderWidth,
          borderRadius: val.shapeBorderRadius,
        };
        break;
    }

    this.updated.emit(updatedBlock);
  }

  asFormControl(control: any): FormControl {
    return control as FormControl;
  }

  addNewVariable(): void {
    const name = this.newVariableName.trim();
    if (!name) return;
    const ctrl = this.form.get('contenu');
    if (ctrl) ctrl.setValue((ctrl.value || '') + `{{${name}}}`);
    this.newVariableName = '';
    this.refreshAvailableVariables();
  }

  get canAddVariable(): boolean {
    const name = this.newVariableName.trim();
    return name.length > 0 && /^[a-zA-Z0-9_]+$/.test(name);
  }

  // ==================== Fusion ====================
  canMergeRight(): boolean {
    if (!this.selectedCell || !this.block?.lignes) return false;
    const { row, col } = this.selectedCell;
    const cell = this.block.lignes[row][col];
    const nextCol = col + (cell.colSpan || 1);
    if (nextCol >= this.tableCols) return false;
    const nextCell = this.block.lignes[row][nextCol];
    return !!nextCell && !nextCell.hidden && (nextCell.colSpan || 1) === 1 && (nextCell.rowSpan || 1) === 1;
  }

  canMergeDown(): boolean {
    if (!this.selectedCell || !this.block?.lignes) return false;
    const { row, col } = this.selectedCell;
    const cell = this.block.lignes[row][col];
    if ((cell.colSpan || 1) !== 1) return false;
    const nextRow = row + (cell.rowSpan || 1);
    if (nextRow >= this.tableRows) return false;
    const nextCell = this.block.lignes[nextRow][col];
    return !!nextCell && !nextCell.hidden && (nextCell.colSpan || 1) === 1 && (nextCell.rowSpan || 1) === 1;
  }

  canUnmerge(): boolean {
    const c = this.selectedCellData;
    return !!c && ((c.colSpan || 1) > 1 || (c.rowSpan || 1) > 1);
  }

  mergeRight(): void {
    if (!this.canMergeRight() || !this.block?.lignes || !this.selectedCell) return;
    const { row, col } = this.selectedCell;
    const newLignes = this.block.lignes.map(r => r.map(c => ({ ...c })));
    const cell = newLignes[row][col];
    const nextCol = col + (cell.colSpan || 1);
    const nextCell = newLignes[row][nextCol];

    cell.colSpan = (cell.colSpan || 1) + (nextCell.colSpan || 1);
    nextCell.hidden = true;

    this.block.lignes = newLignes;
    this.tableChange$.next();
  }

  mergeDown(): void {
    if (!this.canMergeDown() || !this.block?.lignes || !this.selectedCell) return;
    const { row, col } = this.selectedCell;
    const newLignes = this.block.lignes.map(r => r.map(c => ({ ...c })));
    const cell = newLignes[row][col];
    const nextRow = row + (cell.rowSpan || 1);
    const nextCell = newLignes[nextRow][col];

    cell.rowSpan = (cell.rowSpan || 1) + (nextCell.rowSpan || 1);
    nextCell.hidden = true;

    this.block.lignes = newLignes;
    this.tableChange$.next();
  }

  unmergeSelected(): void {
    if (!this.canUnmerge() || !this.block?.lignes || !this.selectedCell) return;
    const { row, col } = this.selectedCell;
    const newLignes = this.block.lignes.map(r => r.map(c => ({ ...c })));
    const cell = newLignes[row][col];

    if (cell.colSpan && cell.colSpan > 1) {
      for (let j = col + 1; j < col + cell.colSpan; j++) {
        if (newLignes[row] && newLignes[row][j]) {
          newLignes[row][j].hidden = false;
        }
      }
    }
    if (cell.rowSpan && cell.rowSpan > 1) {
      for (let i = row + 1; i < row + cell.rowSpan; i++) {
        if (newLignes[i] && newLignes[i][col]) {
          newLignes[i][col].hidden = false;
        }
      }
    }

    cell.colSpan = 1;
    cell.rowSpan = 1;

    this.block.lignes = newLignes;
    this.tableChange$.next();
  }
}