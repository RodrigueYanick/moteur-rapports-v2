import { Component, Input, Output, EventEmitter, SimpleChange, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators, FormsModule, FormControl } from '@angular/forms';
import { DesignBlock } from '../models/design-block.model';
import { debounceTime, Subject } from 'rxjs';
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
  ChevronUp
} from 'lucide-angular';
import { Variable } from '../../models/variable.model';

@Component({
  selector: 'app-block-editor',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, LucideAngularModule],
  templateUrl: './block-editor.html',
  styleUrls: ['./block-editor.scss']
})
export class BlockEditor {
  @Input() block: DesignBlock | null = null;
  @Input() allBlocks: DesignBlock[] = [];
  @Output() updated = new EventEmitter<DesignBlock>();
  @Output() closed = new EventEmitter<void>();
  @Input() explicitVariables: Variable[] = [];
  newVariableName = '';

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
    chevronUp: ChevronUp
  };

  form: FormGroup;
  availableVariables: string[] = [];
  private tableChange$ = new Subject<void>();

  tableRows = 2;
  tableCols = 2;

  // Sections repliables (toutes ouvertes par défaut)
  sectionsOpen: Record<string, boolean> = {
    general: true,
    position: true,
    dimensions: true,
    apparence: true,
    alignement: true,
    typographie: true,
    donnees: true
  };

  fontFamilies = ['Inter', 'Arial', 'Georgia', 'Times New Roman', 'Courier New', 'Roboto'];

  constructor(private fb: FormBuilder) {
    this.form = this.fb.group({
      // Général
      nom: [''],

      // Contenu (titre/texte/tableau/image existants)
      contenu: [''],
      fontSize: [12],
      bold: [false],
      italic: [false],
      underline: [false],
      align: ['left'],
      verticalAlign: ['top'],
      color: ['#000000'],
      fontFamily: ['Inter'],

      // Position
      posX: [0],
      posY: [0],

      // Dimensions
      largeurBox: [200],
      hauteurBox: [50],

      // Apparence
      opacite: [100],
      visible: [true],
      locked: [false],

      // ligne
      epaisseur: [1],
      couleurLigne: ['#000000'],
      largeurLigne: [100],

      // image
      url: [''],
      largeurImage: [100],
      alignImage: ['left'],

      // tableau
      source: [''],
      colonnes: this.fb.array([]),

      // Données
      dataFormat: [''],
      dataDefault: ['']
    });

    this.tableChange$.pipe(debounceTime(300)).subscribe(() => {
      if (this.block) {
        this.updated.emit(this.block);
      }
    });
  }

  toggleSection(key: string): void {
    this.sectionsOpen[key] = !this.sectionsOpen[key];
  }

  updateTableDimensions(): void {
    if (!this.block || this.block.type !== 'tableau') return;
    const newLignes: string[][] = [];
    for (let i = 0; i < this.tableRows; i++) {
      const row: string[] = [];
      for (let j = 0; j < this.tableCols; j++) {
        row.push(this.block.lignes?.[i]?.[j] || '');
      }
      newLignes.push(row);
    }
    this.block = { ...this.block, lignes: newLignes };
    this.tableChange$.next();
  }

  updateCell(row: number, col: number, value: string): void {
    if (!this.block?.lignes) return;
    const newLignes = this.block.lignes.map(r => [...r]);
    newLignes[row][col] = value;
    this.block = { ...this.block, lignes: newLignes };
    this.tableChange$.next();
  }

  trackByIndex(index: number): number {
    return index;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if(changes['explicitVariables']){
      this.refreshAvailableVariables();
    }
    if (this.block) {
      this.form.patchValue({
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
        largeurBox: this.block.largeurBox || 200,
        hauteurBox: this.block.hauteurBox || 50,
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
        dataDefault: this.block.dataBinding?.valeurDefaut || ''
      }, { emitEvent: false });
      this.buildColonnesArray(this.block.colonnes || []);
    }
    if (this.block && this.block.type === 'tableau' && this.block.lignes) {
      this.tableRows = this.block.lignes.length;
      this.tableCols = this.block.lignes[0]?.length || 2;
      if (this.block.source !== undefined || this.block.colonnes !== undefined) {
        this.block = { ...this.block, source: undefined, colonnes: undefined };
      }
    }
  }

  private defaultName(): string {
    if (!this.block) return '';
    return `${this.block.type.charAt(0).toUpperCase()}${this.block.type.slice(1)}`;
  }

  get colonnes(): FormArray {
    return this.form.get('colonnes') as FormArray;
  }

  buildColonnesArray(cols: { titre: string; variable: string }[]): void {
    while (this.colonnes.length) this.colonnes.removeAt(0);
    cols.forEach(col => this.addColonne(col.titre, col.variable));
  }

  addColonne(titre = '', variable = ''): void {
    this.colonnes.push(this.fb.group({
      titre: [titre, Validators.required],
      variable: [variable, Validators.required]
    }));
  }

  removeColonne(index: number): void {
    this.colonnes.removeAt(index);
  }

  refreshAvailableVariables(): void {
    const vars = new Set<string>();
    for (const b of this.allBlocks) {
      const texte = b.contenu || b.url || '';
      const matches = texte.match(/\{\{(.+?)\}\}/g);
      if (matches) {
        matches.forEach(m => vars.add(m.replace('{{', '').replace('}}', '').trim()));
      }
    }
    for(const v of this.explicitVariables){
      vars.add(v.nomVariable);
    }
    this.availableVariables = Array.from(vars).sort();
  }

  insertVariable(varName: string): void {
    const ctrl = this.form.get('contenu');
    if (!ctrl) return;

    const current = (ctrl.value || '').trim();
    // Si le contenu est encore le placeholder auto-généré (ex: {{titre_1}}), on le remplace
    // plutôt que de l'accumuler avec la nouvelle variable.
    const isDefaultPlaceholder = /^\{\{[a-zA-Z0-9_]+\}\}$/.test(current);
    const newValue = isDefaultPlaceholder ? `{{${varName}}}` : current + `{{${varName}}}`;

    ctrl.setValue(newValue);
    this.save();  // applique immédiatement au bloc — plus besoin de cliquer "Enregistrer" séparément
  }

  insertVariableIntoUrl(varName: string): void {
    const urlCtrl = this.form.get('url');
    if(urlCtrl){
      urlCtrl.setValue((urlCtrl.value || '') + `{{${varName}}`);
    }
  }

  // Détecte la variable principale utilisée par ce bloc (pour la section Données)
  get detectedVariable(): string | null {
    if (!this.block) return null;
    const texte = this.block.type === 'image' ? (this.block.url || '') : (this.block.contenu || '');
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
      colonnes: val.colonnes || [],
      dataBinding: {
        format: val.dataFormat,
        valeurDefaut: val.dataDefault
      }
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
        updatedBlock.source = val.source;
        updatedBlock.colonnes = val.colonnes;
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
    if (ctrl) {
      ctrl.setValue((ctrl.value || '') + `{{${name}}}`);
    }
    this.newVariableName = '';
    this.refreshAvailableVariables();
  }

  get canAddVariable(): boolean {
    const name = this.newVariableName.trim();
    return name.length > 0 && /^[a-zA-Z0-9_]+$/.test(name);
  }
}