import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormBuilder, Validators, FormsModule } from '@angular/forms';
import { Template } from '../../models/template.model';
import { Variable } from '../../models/variable.model';
import { TemplateApiService } from '../../services/template-api';
import { ActivatedRoute, Router } from '@angular/router';
import { TemplateSchema } from '../../models/template-schema.model';

@Component({
  selector: 'app-template-detail',
  standalone: true,
  imports: [ CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './template-detail.html',
  styleUrls: ['./template-detail.scss'],
})
export class TemplateDetail implements OnInit {

  template: Template | null = null;
  variables: Variable[] = [];
  variableForm: FormGroup;
  jsonData: string = '';
  generationError: string = ''
  loading = false;
  error = '';
  schema: TemplateSchema | null = null;
  

  constructor (
    private fb: FormBuilder,
    private api : TemplateApiService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ){
    this.variableForm = this.fb.group({
      nomVariable: ['', Validators.required],
      type: ['STRING', Validators.required],
      obligatoire: [false]
    });
  }

  ngOnInit(): void {
    this.loadTemplate();
  }

  loadSchema(): void{
    if(!this.template) return;
    this.api.getSchema(this.template.id).subscribe({
      next: (schema) =>this.schema = schema,
      error: (err) => {
        console.error("Erreur lor du chargement", err);
        this.schema = null;
      }
    });
  }

  loadTemplate(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if(!id) return;
    this.loading = true;
    this.api.getTemplate(id).subscribe({
      next: (template) => {
        this.template = template;
        this.loading = false;
        if(this.template.statut === 'PUBLIE'){
          this.loadSchema();
        }
      },
      error: (err) => {
        this.error = "Impossible de charger les modele";
        this.loading = false;
        console.error(err);
        this.cdr.detectChanges();
      }
    });
  }

  // loadVariables(templateId: string): void {
  //   this.api.getVariables(templateId).subscribe({
  //     next: (variables) => {
  //       this.variables = variables;
  //       this.cdr.detectChanges();
  //     }, 
  //     error: (err) => {
  //       console.error(err);
  //       this.cdr.detectChanges();
  //     }
  //   })
  // }

  publish(): void {
    if(!this.template) return 
    this.api.publishTemplate(this.template.id).subscribe({
      next: (updated) => {
        this.template = updated;
        // recharger les templates extrait automatiquement 
        this.loadSchema();
      },
      error: err => {
        alert('echec de la publication.');
        console.error(err);
      }
    });
  }

  // addVariable(): void {
  //   if (!this.template || this.variableForm.invalid) return;
  //   const formVal = this.variableForm.value;
  //   const newVar = {
  //     nomVariable: formVal.nomVariable,
  //     type: formVal.type,
  //     obligatoire: formVal.obligatoire
  //   };
  //   this.api.addVariable(this.template.id, newVar).subscribe({
  //     next: () => {
  //       this.loadVariables(this.template!.id);
  //       this.variableForm.reset({ type: 'STRING', obligatoire: false });
  //     },
  //     error: (err) => {
  //       alert('Erreur lors de l\'ajout de la variable (peut-être un doublon ou template publié).');
  //       console.error(err);
  //     }
  //   });
  // }

  // deleteVariable(variableId: string): void {
  //   if (!this.template) return;
  //   this.api.deleteVariable(this.template.id, variableId).subscribe({
  //     next: () => this.loadVariables(this.template!.id),
  //     error: (err) => console.error(err)
  //   });
  // }

generate(): void {
  if (!this.template) return;
  this.generationError = '';
  let data: any;
  try {
    data = JSON.parse(this.jsonData);
  } catch (e) {
    this.generationError = 'JSON invalide.';
    return;
  }

  this.api.generateDocument(this.template.id, data).subscribe({
    next: (blob: Blob) => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${this.template!.nom || 'document'}.pdf`;
      a.click();
      window.URL.revokeObjectURL(url);
    },
    error: (err) => {
      // Extraction des erreurs de validation (400 Bad Request)
      if (err.status === 400 && err.error) {
        const body = err.error;
        if (body.errors && Array.isArray(body.errors)) {
          // Liste d'erreurs renvoyée par ValidationException
          this.generationError = body.errors.join('\n');
        } else if (body.message) {
          this.generationError = body.message;
        } else {
          this.generationError = 'Erreur de validation.';
        }
      } else {
        this.generationError = 'Erreur lors de la génération. Vérifiez les données et que le template est publié.';
      }
      console.error(err);
    }
  });
}

}
