import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { TemplateApiService } from '../../services/template-api';
import { Variable } from '../../models/variable.model';
import { GeneratedDocument } from '../../models/Document.model';
import { FillerDataService } from '../services/filler-data';
import { Subject, takeUntil } from 'rxjs';
import { DesignBlock } from '../models/design-block.model';
import {
  LucideAngularModule, ClipboardList, Plus, Save, Loader2, FolderOpen, Printer, Trash2
} from 'lucide-angular';

@Component({
  selector: 'app-template-filler',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './template-filler.html',
  styleUrls: ['./template-filler.scss'],
})
export class TemplateFiller implements OnInit, OnDestroy {
  @Input() templateId: string | null = null;
  @Input() selectedBlock: DesignBlock | null = null;
  @Input() allBlocks: DesignBlock[] = [];

  @Output() generatePdfRequest = new EventEmitter<Record<string, any>>();

  variables: Variable[] = [];
  loading = false;
  error: string | null = null;
  values: Record<string, any> = {};

  documents: GeneratedDocument[] = [];
  loadingDocuments = false;
  docNameInput = '';
  savingDocument = false;
  activeDocumentId: string | null = null;
  showAllVariables = false;

  private destroy$ = new Subject<void>();


  readonly icons = {
    clipboard: ClipboardList,
    plus: Plus,
    save: Save,
    loader: Loader2,
    folder: FolderOpen,
    printer: Printer,
    trash: Trash2
  };

  constructor(
    private api: TemplateApiService,
    private fillerData: FillerDataService,
    private cdr: ChangeDetectorRef
  ) {
    this.fillerData.values$.subscribe((values) => {
      this.values = { ...values };
      this.cdr.detectChanges();
    });
  }

  ngOnInit(): void {
    if (this.templateId) {
      this.loadVariables();
      this.loadDocuments();
    }
  }

  ngOnChanges(changes: any): void {
    if (changes['templateId'] && this.templateId) {
      this.loadVariables();
      this.loadDocuments();
    }
  }

  loadVariables(): void {
    if (!this.templateId) return;
    this.loading = true;
    this.error = null;
    this.api.getVariables(this.templateId).subscribe({
      next: (vars) => {
        this.variables = vars;
        this.initValues();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des variables';
        this.loading = false;
        console.error(err);
        this.cdr.detectChanges();
      },
    });
  }

  private initValues(): void {
    const existing = this.fillerData.getValues();
    const merged: Record<string, any> = { ...existing };
    for (const v of this.variables) {
      if (!(v.nomVariable in merged)) {
        merged[v.nomVariable] = v.type === 'ARRAY' ? [] : '';
      }
    }
    this.values = merged;
    this.fillerData.setValues(merged);
  }

  onValueChange(varName: string, event: any): void {
    const rawValue = event.target?.value ?? event;
    const variable = this.variables.find((v) => v.nomVariable === varName);

    let value: any = rawValue;
    if (variable?.type === 'FLOAT') {
      value = rawValue === '' ? null : Number(rawValue);
      if (value !== null && isNaN(value)) value = null;
    } else if (variable?.type === 'BOOLEAN') {
      value = !!rawValue;
    }

    this.values[varName] = value;
    this.fillerData.updateValue(varName, value);
    this.cdr.detectChanges();
  }

  addRow(varName: string): void {
    if (!this.values[varName]) this.values[varName] = [];
    const newRow: Record<string, any> = {};
    const colonnes = this.getArrayColumns(varName);

    if (colonnes.length > 0) {
      for (const col of colonnes) {
        newRow[col] = '';
      }
    } else {
      // Fallback très générique si aucune colonne trouvée
      newRow['col1'] = '';
      newRow['col2'] = '';
    }
    this.values[varName].push(newRow);
    this.fillerData.updateValue(varName, this.values[varName]);
    this.cdr.detectChanges();
  }

    /** Retourne les noms de colonnes pour une variable ARRAY, en cherchant dans tous les blocs. */
  getArrayColumns(varName: string): string[] {
    // Cherche un bloc tableau dynamique dont la source correspond
    const block = this.allBlocks.find(
      (b) => b.type === 'tableau' && b.source && b.source.replace('{{', '').replace('}}', '').trim() === varName
    );
    // if (block?.colonnes) {
    //   return block.colonnes.filter(c => c.variable).map(c => c.variable);
    // }
    // // Sinon, utilise selectedBlock si c'est le bon tableau
    // if (this.selectedBlock?.type === 'tableau' && this.selectedBlock.source?.replace('{{', '').replace('}}', '').trim() === varName) {
    //   return (this.selectedBlock.colonnes || []).filter(c => c.variable).map(c => c.variable);
    // }
    // Fallback : clés de la première ligne existante
    const firstRow = this.values[varName]?.[0];
    if (firstRow && typeof firstRow === 'object') {
      return Object.keys(firstRow);
    }
    return [];
  }

  /** Suivi des lignes */
  trackByRow(index: number): number {
    return index;
  }

  /** Suivi des colonnes */
  trackByCol(index: number, col: string): string {
    return col;
  }

  // ================== DOCUMENTS SAUVEGARDÉS ==================

  loadDocuments(): void {
    if (!this.templateId) return;
    this.loadingDocuments = true;
    this.api.getDocuments(this.templateId).subscribe({
      next: (docs) => {
        this.documents = docs;
        this.loadingDocuments = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur chargement documents', err);
        this.loadingDocuments = false;
        this.cdr.detectChanges();
      },
    });
  }

  saveDocument(): void {
    if (!this.templateId || this.savingDocument) return;
    const nom = this.docNameInput.trim() || `Document du ${new Date().toLocaleDateString()}`;
    this.savingDocument = true;

    const request$ = this.activeDocumentId
      ? this.api.updateDocument(this.templateId, this.activeDocumentId, {
          nom,
          donnees: this.values,
        })
      : this.api.createDocument(this.templateId, { nom, donnees: this.values });

    request$.subscribe({
      next: (doc) => {
        this.activeDocumentId = doc.id;
        this.docNameInput = doc.nom;
        this.savingDocument = false;
        this.loadDocuments();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur sauvegarde document', err);
        this.savingDocument = false;
        this.cdr.detectChanges();
      },
    });
  }

  loadDocument(doc: GeneratedDocument): void {
    this.activeDocumentId = doc.id;
    this.docNameInput = doc.nom;
    this.values = { ...doc.donnees };
    this.fillerData.setValues(this.values);
    this.cdr.detectChanges();
  }

  generateFromDocument(doc: GeneratedDocument): void {
    this.loadDocument(doc);
    this.generatePdfRequest.emit(doc.donnees);
  }

  deleteDocument(doc: GeneratedDocument): void {
    if (!this.templateId || !confirm(`Supprimer le document "${doc.nom}" ?`)) return;
    this.api.deleteDocument(this.templateId, doc.id).subscribe({
      next: () => {
        this.documents = this.documents.filter((d) => d.id !== doc.id);
        if (this.activeDocumentId === doc.id) {
          this.activeDocumentId = null;
          this.docNameInput = '';
          this.initValues();
          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        console.error('Erreur suppression document', err);
        this.cdr.detectChanges();
      }
    });
  }

  newDocument(): void {
    this.activeDocumentId = null;
    this.docNameInput = '';
    this.initValues();
    this.cdr.detectChanges();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  get filteredVariables(): Variable[] {
    if (this.showAllVariables || !this.selectedBlock) return this.variables;
    const names = this.extractBlockVariableNames(this.selectedBlock);
    return this.variables.filter((v) => names.has(v.nomVariable));
  }

  private extractBlockVariableNames(block: DesignBlock): Set<string> {
    const names = new Set<string>();

    const scan = (text?: string) => {
      if (!text) return;
      const matches = text.match(/\{\{(.+?)\}\}/g);
      if (matches) matches.forEach((m) => names.add(m.replace(/\{\{|\}\}/g, '').trim()));
    };
    scan(block.contenu);
    scan(block.url);
    if (block.type === 'tableau' && block.source)
      names.add(block.source.replace(/\{\{|\}\}/g, '').trim());
    if (block.lignes) {
      for (const row of block.lignes) for (const cell of row) scan(cell.value);
    }
    return names;
  }
}
