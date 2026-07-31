import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { TemplateApiService } from '../../services/template-api';
import { Variable } from '../../models/variable.model';
import { FillerDataService } from '../services/filler-data';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-template-filler',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './template-filler.html',
  styleUrls: ['./template-filler.scss']
})
export class TemplateFiller implements OnInit, OnDestroy {
  @Input() templateId: string | null = null;

  variables: Variable[] = [];
  loading = false;
  error: string | null = null;

  // Valeurs locales liées aux champs
  values: Record<string, any> = {};

  private destroy$ = new Subject<void>();

  constructor(
    private api: TemplateApiService,
    private fillerData: FillerDataService
  ) {}

  ngOnInit(): void {
    if (this.templateId) {
      this.loadVariables();
    }
  }

  ngOnChanges(changes: any): void {
    if (changes['templateId'] && this.templateId) {
      this.loadVariables();
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
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des variables';
        this.loading = false;
        console.error(err);
      }
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
    const variable = this.variables.find(v => v.nomVariable === varName);

    let value: any = rawValue;

    if (variable?.type === 'FLOAT') {
      value = rawValue === '' ? null : Number(rawValue);
      if (value !== null && isNaN(value)) value = null;
    } else if (variable?.type === 'BOOLEAN') {
      value = !!rawValue;
    }

    this.values[varName] = value;
    this.fillerData.updateValue(varName, value);
  }

  // Pour les tableaux, on peut ajouter une ligne (méthode simplifiée)
  addRow(varName: string): void {
    if (!this.values[varName]) this.values[varName] = [];
    // Crée une ligne avec des clés génériques (col1, col2, etc.)
    const newRow: any = {};
    const nbCols = 2; // valeur par défaut, à adapter selon la variable
    for (let i = 1; i <= nbCols; i++) {
      newRow['col' + i] = '';
    }
    this.values[varName].push(newRow);
    this.fillerData.updateValue(varName, this.values[varName]);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  saveDocument(): void {
    if (!this.templateId) return;
    const nom = prompt('Nom du document :', 'Document sans titre');
    if (!nom) return;
    this.api.createDocument(this.templateId, { nom, donnees: this.values }).subscribe({
      next: (doc) => alert('Document sauvegardé avec succès !'),
      error: (err) => console.error('Erreur sauvegarde document', err)
    });
  }
}