import {
  Component,
  Input,
  OnInit,
  OnDestroy,
  Output,
  EventEmitter,
  SimpleChanges,
  ChangeDetectorRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators,
  FormsModule,
} from '@angular/forms';
import { TemplateApiService } from '../../services/template-api';
import { Variable } from '../../models/variable.model';
import { Subject } from 'rxjs';
import {
  LucideAngularModule, Braces, Sigma, Edit3, Trash2, Plus
} from 'lucide-angular';

@Component({
  selector: 'app-variable-manager',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, LucideAngularModule],
  templateUrl: './variable-manager.html',
  styleUrls: ['./variable-manager.scss'],
})
export class VariableManager implements OnInit, OnDestroy {
  @Input() templateId: string | null = null;
  @Output() variablesLoaded = new EventEmitter<Variable[]>();
  @Output() insertVariable = new EventEmitter<Variable>();
  
  readonly icons = {
    variable: Braces,
    formula: Sigma,
    edit: Edit3,
    trash: Trash2,
    plus: Plus,
  };

  insert(v: Variable): void {
    this.insertVariable.emit(v);
  }

  variables: Variable[] = [];
  loading = false;
  error: string | null = null;

  form: FormGroup;
  editingVariableId: string | null = null;

  private loadedOnce = false;
  private destroy$ = new Subject<void>();
  
  constructor(
    private fb: FormBuilder,
    private api: TemplateApiService,
    private cdr: ChangeDetectorRef,
  ) {
    this.form = this.fb.group({
      nomVariable: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9_]+$/)]],
      type: ['STRING', Validators.required],
      description: [''],
      obligatoire: [false],
    });
  }

  ngOnInit(): void {
    if (!this.loadedOnce && this.templateId) {
      this.loadedOnce = true;
      this.loadVariables();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['templateId'] && this.templateId) {
      this.loadedOnce = true;
      this.loadVariables();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadVariables(): void {
    if (!this.templateId) return;
    this.loading = true;
    this.error = null;
    this.api.getVariables(this.templateId).subscribe({
      next: (vars) => {
        this.variables = vars;
        this.loading = false;
        this.cdr.markForCheck();
        queueMicrotask(() => {
          this.variablesLoaded.emit(vars);
        });
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des variables';
        this.loading = false;
        this.cdr.markForCheck();
        console.error(err);
      },
    });
  }

  save(): void {
    console.log('save() appelé, templateId =', this.templateId);
    if (!this.templateId || this.form.invalid) {
      console.log('Formulaire invalide ou templateId manquant');
      return;
    }
    const val = this.form.value;
    // Backend enum does not support 'IMAGE' — map it to 'STRING' for transport
    const mappedType = val.type === 'IMAGE' ? 'STRING' : val.type;
    const dto = {
      nomVariable: val.nomVariable,
      type: mappedType,
      obligatoire: val.obligatoire,
      description: val.description,
    };

    if (this.editingVariableId) {
      this.api.updateVariable(this.templateId, this.editingVariableId, dto).subscribe({
        next: (updated) => {
          const idx = this.variables.findIndex((v) => v.id === updated.id);
          if (idx !== -1) this.variables[idx] = updated;
          else this.variables.push(updated);
          this.resetForm();
          this.cdr.markForCheck();
          queueMicrotask(() => {
            this.variablesLoaded.emit(this.variables);
          });
        },
        error: (err) => {
          this.error = err.error?.message || 'Erreur lors de la mise à jour';
          this.cdr.markForCheck();
          console.error(err);
        },
      });
    } else {
      this.api.addVariable(this.templateId, dto).subscribe({
        next: (created) => {
          this.variables.push(created);
          this.resetForm();
          this.cdr.markForCheck();
          queueMicrotask(() => {
            this.variablesLoaded.emit(this.variables);
          });
        },
        error: (err) => {
          this.error = err.error?.message || 'Erreur lors de la création';
          this.cdr.markForCheck();
          console.error(err);
        },
      });
    }
  }

  edit(variable: Variable): void {
    this.editingVariableId = variable.id;
    this.form.patchValue({
      nomVariable: variable.nomVariable,
      type: variable.type,
      description: variable.description || '',
      obligatoire: variable.obligatoire,
    });
    this.cdr.markForCheck();
  }

  delete(variable: Variable): void {
    if (!this.templateId || !confirm(`Supprimer la variable "${variable.nomVariable}" ?`)) return;
    this.api.deleteVariable(this.templateId, variable.id).subscribe({
      next: () => {
        this.variables = this.variables.filter((v) => v.id !== variable.id);
        if (this.editingVariableId === variable.id) this.resetForm();
        this.cdr.markForCheck();
        queueMicrotask(() => {
          this.variablesLoaded.emit(this.variables);
        });
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors de la suppression';
        this.cdr.markForCheck();
        console.error(err);
      },
    });
  }

  resetForm(): void {
    this.form.reset({ type: 'STRING', obligatoire: false });
    this.editingVariableId = null;
  }
}
