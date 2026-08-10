import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnChanges,
  SimpleChanges,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TemplateApiService } from '../../services/template-api';
import { Variable } from '../../models/variable.model';
import { GeneratedDocument } from '../../models/Document.model';
import { FillerDataService } from '../../designer/services/filler-data';
import {
  LucideAngularModule,
  CheckCircle2,
  Save,
  FolderOpen,
  Trash2,
  AlertCircle,
  FileCheck2,
} from 'lucide-angular';

@Component({
  selector: 'app-variable-validation-form',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './variable-validation-form.html',
  styleUrls: ['./variable-validation-form.scss'],
})
export class VariableValidationForm implements OnInit, OnChanges {
  @Input() templateId: string | null = null;
  @Input() active = false;
  @Output() validated = new EventEmitter<void>();

  readonly icons = {
    check: CheckCircle2,
    save: Save,
    folder: FolderOpen,
    trash: Trash2,
    alert: AlertCircle,
    fileCheck: FileCheck2,
  };

  variables: Variable[] = [];
  values: Record<string, any> = {};
  loading = false;
  error: string | null = null;

  documents: GeneratedDocument[] = [];
  activeDocumentId: string | null = null;
  docNameInput = '';
  saving = false;

  formVisible = true;

  constructor(
    private api: TemplateApiService,
    private fillerData: FillerDataService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    if (this.active && this.templateId) this.load();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['active'] && this.active && this.templateId) {
      this.formVisible = true;
      this.load();
      this.cdr.detectChanges(); // 🔧 force la détection de changement après le chargement
    }
  }

  private load(): void {
    if (!this.templateId) return;
    this.loading = true;
    this.error = null;
    // Récupère le schéma (variables) depuis l’API
    this.api.getSchema(this.templateId).subscribe({
      next: (schema) => {
        this.variables = schema.variables;
        this.initValues(); // 🔧 modification : fusionne avec les valeurs existantes
        this.loading = false;
        this.cdr.detectChanges(); // 🔧 force la détection de changement après la mise à jour des variables
      },
      error: (err) => {
        this.error = 'Impossible de charger le schéma de variables.';
        this.loading = false;
        this.cdr.detectChanges(); // 🔧 force la détection de changement après l’erreur
        console.error(err);
      },
    });
    this.loadDocuments();
  }

  // 🔧 Initialise les valeurs en conservant ce qui a déjà été saisi via le FillerDataService
  private initValues(): void {
    const existingValues = this.fillerData.getValues(); // récupère l'état actuel
    const values: Record<string, any> = { ...existingValues };
    for (const v of this.variables) {
      if (!(v.nomVariable in values)) {
        values[v.nomVariable] = v.type === 'ARRAY' ? [] : '';
      }
    }
    this.values = values;
  }

  // 🔧 Met à jour localement ET dans le service pour un rendu en temps réel
  onValueChange(varName: string, event: any): void {
    const raw = event.target?.value ?? event;
    const variable = this.variables.find((v) => v.nomVariable === varName);
    let value: any = raw;
    if (variable?.type === 'FLOAT') {
      value = raw === '' ? null : Number(raw);
      if (value !== null && isNaN(value)) value = null;
    } else if (variable?.type === 'BOOLEAN') {
      value = !!raw;
    }
    this.values[varName] = value;
    this.fillerData.updateValue(varName, value); // 🔧 synchronise avec le service
    this.cdr.detectChanges();
  }

  addRow(varName: string): void {
    if (!this.values[varName]) this.values[varName] = [];
    this.values[varName].push({ col1: '', col2: '' });
    this.fillerData.updateValue(varName, this.values[varName]);
    this.cdr.detectChanges();
  }

  // ---------- Documents ----------

  loadDocuments(): void {
    if (!this.templateId) return;
    this.api.getDocuments(this.templateId).subscribe({
      next: (docs) => {
        this.documents = docs;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur chargement documents', err);
        this.cdr.detectChanges();
      },
    });
  }

  saveDocument(): void {
    if (!this.templateId || this.saving) return;
    const nom = this.docNameInput.trim() || `Document du ${new Date().toLocaleDateString()}`;
    this.saving = true;

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
        this.saving = false;
        this.loadDocuments();
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur sauvegarde document', err);
        this.saving = false;
        this.cdr.detectChanges();
      },
    });
  }

  loadDocument(doc: GeneratedDocument): void {
    this.activeDocumentId = doc.id;
    this.docNameInput = doc.nom;
    this.values = { ...doc.donnees };
    this.fillerData.setValues(this.values); // 🔧 synchronise aussi
    this.formVisible = true;
    this.cdr.detectChanges();
  }

  deleteDocument(doc: GeneratedDocument): void {
    if (!this.templateId || !confirm(`Supprimer "${doc.nom}" ?`)) return;
    this.api.deleteDocument(this.templateId, doc.id).subscribe({
      next: () => {
        this.documents = this.documents.filter((d) => d.id !== doc.id);
        if (this.activeDocumentId === doc.id) this.activeDocumentId = null;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.cdr.detectChanges();
      },
    });
  }

  // ---------- Validation ----------

  get missingRequired(): string[] {
    return this.variables
      .filter(
        (v) =>
          v.obligatoire &&
          (this.values[v.nomVariable] === '' || this.values[v.nomVariable] == null),
      )
      .map((v) => v.nomVariable);
  }

  validate(): void {
    if (this.missingRequired.length > 0) return;
    this.fillerData.setValues({ ...this.values }); // 🔧 pousse les valeurs définitives
    this.formVisible = false;
    this.validated.emit();
    this.cdr.detectChanges();
  }

  cancel(): void {
    this.formVisible = false;
    this.cdr.detectChanges();
  }
}
