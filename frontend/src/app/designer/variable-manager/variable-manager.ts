import {
  Component,
  Input,
  OnInit,
  OnDestroy,
  Output,
  EventEmitter,
  SimpleChanges,
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

@Component({
  selector: 'app-variable-manager',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './variable-manager.html',
  styleUrls: ['./variable-manager.scss'],
})
export class VariableManager implements OnInit, OnDestroy {
  @Input() templateId: string | null = null;
  @Output() variablesLoaded = new EventEmitter<Variable[]>();

  variables: Variable[] = [];
  loading = false;
  error: string | null = null;

  form: FormGroup;
  editingVariableId: string | null = null;

  
  private destroy$ = new Subject<void>();
  
  constructor(
    private fb: FormBuilder,
    private api: TemplateApiService,
  ) {
    this.form = this.fb.group({
      nomVariable: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9_]+$/)]],
      type: ['STRING', Validators.required],
      description: [''],
      obligatoire: [false],
    });
  }

  ngOnInit(): void {
    this.loadVariablesIfIdPresent();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['templateId'] && this.templateId) {
      this.loadVariables();
    }
  }

  private loadVariablesIfIdPresent(): void {
    if (this.templateId) {
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
        this.variablesLoaded.emit(vars);
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des variables';
        this.loading = false;
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
          this.variablesLoaded.emit(this.variables);
          this.resetForm();
        },
        error: (err) => {
          this.error = err.error?.message || 'Erreur lors de la mise à jour';
          console.error(err);
        },
      });
    } else {
      this.api.addVariable(this.templateId, dto).subscribe({
        next: (created) => {
          this.variables.push(created);
          this.variablesLoaded.emit(this.variables);
          this.resetForm();
        },
        error: (err) => {
          this.error = err.error?.message || 'Erreur lors de la création';
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
  }

  delete(variable: Variable): void {
    if (!this.templateId || !confirm(`Supprimer la variable "${variable.nomVariable}" ?`)) return;
    this.api.deleteVariable(this.templateId, variable.id).subscribe({
      next: () => {
        this.variables = this.variables.filter((v) => v.id !== variable.id);
        if (this.editingVariableId === variable.id) this.resetForm();
        this.variablesLoaded.emit(this.variables);
      },
      error: (err) => {
        this.error = err.error?.message || 'Erreur lors de la suppression';
        console.error(err);
      },
    });
  }

  resetForm(): void {
    this.form.reset({ type: 'STRING', obligatoire: false });
    this.editingVariableId = null;
  }
}
