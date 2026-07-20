import { Component, OnInit } from '@angular/core';
import { NgIf, NgForOf, CommonModule } from "../../../../node_modules/@angular/common/types/_common_module-chunk";
import { ReactiveFormsModule, ɵInternalFormsSharedModule, FormGroup, FormBuilder, Validators } from "@angular/forms";
import { Template } from '../../models/template.model';
import { Variable } from '../../models/variable.model';
import { TemplateApiService } from '../../services/template-api';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-template-detail',
  standalone: true,
  imports: [ ɵInternalFormsSharedModule, CommonModule, ReactiveFormsModule],
  templateUrl: './template-detail.html',
  styleUrl: './template-detail.scss',
})
export class TemplateDetail implements OnInit {

  template: Template | null = null;
  variables: Variable[] = [];
  variableForm: FormGroup;
  jsonData: string = '';
  generationError: string = ''
  loading = false;
  error = ''
  

  constructor (
    private fb: FormBuilder,
    private api : TemplateApiService,
    private route: ActivatedRoute
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

  loadTemplate(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if(!id) return;
    this.loading = true;
    this.api.getTemplate(id).subscribe({
      next: (template) => {
        this.template = template;
        this.loading = false;
        this.loadVariables(id)
      },
      error: (err) => {
        this.error = "Impossible de charger les modele";
        this.loading = false;
        console.error(err);
      }
    });
  }

  loadVariables(templateId: string): void {
    this.api.getVariables(templateId).subscribe({
      next: (variables) => {
        this.variables = variables;
      }, 
      error: (err) => console.error(err)
    })
  }

  publish(): void {
    
  }

}
