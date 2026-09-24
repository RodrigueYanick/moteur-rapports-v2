import { Component, ChangeDetectorRef } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TemplateApiService } from '../../services/template-api';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { forkJoin, of } from 'rxjs';
import {
  LucideAngularModule,
  Receipt,
  ShoppingCart,
  Briefcase,
  BarChart3,
  Award,
  FilePlus,
  Sparkles,
  Check,
  ArrowRight,
  Layers,
  FileText,
  Loader2
} from 'lucide-angular';
import { STARTER_TEMPLATES, StarterTemplate } from '../starter-templates/starter-templates.data';

@Component({
  selector: 'app-template-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, LucideAngularModule],
  templateUrl: './template-create.html',
  styleUrls: ['./template-create.scss'],
})
export class TemplateCreate {
  error: string | null = null;
  submitting = false;
  form: FormGroup;

  // Modèles types (Starter Templates)
  starterTemplates: StarterTemplate[] = STARTER_TEMPLATES;
  selectedCategory: string = 'TOUS';
  selectedStarterId: string = 'blank'; // 'blank' ou id du starter
  selectedStarter: StarterTemplate | null = null;

  readonly icons = {
    receipt: Receipt,
    shoppingCart: ShoppingCart,
    briefcase: Briefcase,
    barChart: BarChart3,
    award: Award,
    filePlus: FilePlus,
    sparkles: Sparkles,
    check: Check,
    arrowRight: ArrowRight,
    layers: Layers,
    fileText: FileText,
    loader: Loader2,
  };

  constructor(
    private fb: FormBuilder,
    private api: TemplateApiService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.form = this.fb.group({
      nom: ['', Validators.required],
      description: [''],
      categorie: ['AUTRES'],
      modePagination: ['FIXED'],
    });
  }

  get filteredStarters(): StarterTemplate[] {
    if (this.selectedCategory === 'TOUS') {
      return this.starterTemplates;
    }
    return this.starterTemplates.filter(t => t.categorie === this.selectedCategory);
  }

  getStarterIcon(iconName: string): any {
    switch (iconName) {
      case 'receipt': return this.icons.receipt;
      case 'shopping-cart': return this.icons.shoppingCart;
      case 'briefcase': return this.icons.briefcase;
      case 'bar-chart-3': return this.icons.barChart;
      case 'award': return this.icons.award;
      default: return this.icons.fileText;
    }
  }

  selectStarter(starter: StarterTemplate | 'blank'): void {
    if (starter === 'blank') {
      this.selectedStarterId = 'blank';
      this.selectedStarter = null;
      this.form.patchValue({
        nom: '',
        description: '',
        categorie: 'AUTRES',
        modePagination: 'FIXED',
      });
    } else {
      this.selectedStarterId = starter.id;
      this.selectedStarter = starter;
      this.form.patchValue({
        nom: starter.nom,
        description: starter.description,
        categorie: starter.categorie,
        modePagination: starter.modePagination,
      });
    }
    this.cdr.detectChanges();
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.error = '';
    this.cdr.detectChanges();

    const formValues = this.form.value;
    const payload: any = {
      ...formValues,
    };

    if (this.selectedStarter && this.selectedStarter.pages) {
      payload.contenuDesign = JSON.stringify({
        pages: this.selectedStarter.pages.map(p => ({
          nom: p.nom,
          blocs: p.blocks,
        })),
      });
    }

    this.api.createTemplate(payload).subscribe({
      next: (template) => {
        // Si un starter template a été choisi avec des variables, on les injecte dans le schéma
        if (this.selectedStarter && this.selectedStarter.variables && this.selectedStarter.variables.length > 0) {
          const varObservables = this.selectedStarter.variables.map(v =>
            this.api.addVariable(template.id, {
              nomVariable: v.nomVariable,
              type: v.type,
              obligatoire: v.obligatoire,
              description: v.description,
            })
          );

          forkJoin(varObservables).subscribe({
            next: () => {
              this.router.navigate(['/templates', template.id]);
            },
            error: (err) => {
              console.warn('Avertissement : certaines variables n’ont pu être pré-créées :', err);
              this.router.navigate(['/templates', template.id]);
            },
          });
        } else {
          this.router.navigate(['/templates', template.id]);
        }
      },
      error: (err) => {
        this.error = 'Erreur lors de la création du modèle.';
        this.submitting = false;
        this.cdr.detectChanges();
        console.error(err);
      },
    });
  }
}
