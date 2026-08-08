import { Component, OnInit, ChangeDetectorRef, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormBuilder, Validators, FormsModule } from '@angular/forms';
import { Template } from '../../models/template.model';
import { Variable } from '../../models/variable.model';
import { TemplateApiService } from '../../services/template-api';
import { ActivatedRoute, Router } from '@angular/router';
import { TemplateSchema } from '../../models/template-schema.model';
import { ReportDesigner } from '../../designer/report-designer/report-designer';
import { DesignBlock } from '../../designer/models/design-block.model';
import { DesignSerializer } from '../../designer/services/design-serializer.service';
import { BlockEditor } from '../../designer/block-editor/block-editor';
import { Subject, debounceTime } from 'rxjs';
import { MockDataService } from '../../designer/services/mock-data.service';

@Component({
  selector: 'app-template-detail',
  standalone: true,
  imports: [ CommonModule, ReactiveFormsModule, FormsModule, ReportDesigner],
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
  publishing = false;
  blocks: DesignBlock[] = [];
  savingStatus: 'idle' | 'saving' | 'saved' = 'idle';
  private saveSubject = new Subject<void>();
  templateId: string|null = null;
  

  constructor (
    private fb: FormBuilder,
    private api : TemplateApiService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef,
    private serializer: DesignSerializer,
    private mockDataService: MockDataService
  ){
    this.variableForm = this.fb.group({
      nomVariable: ['', Validators.required],
      type: ['STRING', Validators.required],
      obligatoire: [false]
    });
  }

  ngOnInit(): void {
    this.loadTemplate();

    // Auto‑save avec debounce de 2 secondes
    this.saveSubject.pipe(debounceTime(2000)).subscribe(() => {
      this.doSave();
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
      if (changes['templateId']) {
          console.log('Sidebar templateId reçu :', this.templateId);
      }
  }

  generateWithMockData(dataOverride?: any): void {
    if (!this.template) return;
    if (this.template.statut !== 'PUBLIE') {
      this.generationError = 'Le template doit être publié avant de pouvoir générer un document.';
      this.cdr.detectChanges();
      return;
    }

    // Récupère d'abord les variables explicites du template
    this.api.getVariables(this.template.id).subscribe({
      next: (variables) => {
        // Prépare un objet avec des valeurs par défaut pour toutes les variables
        const defaultData: Record<string, any> = {};
        for (const v of variables) {
          switch (v.type) {
            case 'ARRAY': defaultData[v.nomVariable] = []; break;
            case 'FLOAT': defaultData[v.nomVariable] = 0; break;
            case 'BOOLEAN': defaultData[v.nomVariable] = false; break;
            case 'IMAGE': defaultData[v.nomVariable] = false; break;
            default: defaultData[v.nomVariable] = '';
          }
        }

        // Fusionne avec les données passées (priorité aux valeurs réelles)
        const data = { ...defaultData, ...(dataOverride || this.mockDataService.generate(this.blocks)) };

        this.generationError = '';
        this.api.generateDocument(this.template!.id, data).subscribe({
          next: (blob: Blob) => {
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `${this.template!.nom || 'document'}.pdf`;
            a.click();
            window.URL.revokeObjectURL(url);
          },
          error: (err) => this.handleGenerationError(err)
        });
      },
      error: (err) => {
        this.generationError = 'Impossible de charger les variables du template.';
        console.error(err);
      }
    });
  }

  private handleGenerationError(err: any): void {
    if (err.status === 400 && err.error) {
      const reader = new FileReader();
      reader.onload = () => {
        try {
          const body = JSON.parse(reader.result as string);
          if (body.errors && Array.isArray(body.errors)) {
            this.generationError = body.errors.join('\n');
          } else if (body.message) {
            this.generationError = body.message;
          } else {
            this.generationError = 'Erreur de validation.';
          }
        } catch (e) {
          this.generationError = 'Erreur de validation (réponse non lisible).';
        }
      };
      reader.readAsText(err.error);
    } else {
      this.generationError = 'Erreur lors de la génération. Vérifiez les données et que le template est publié.';
    }
    console.error(err);
  }

  onExportRequest(data?: any): void {
    if (data) {
      this.generateWithMockData(data);   // données réelles
    } else {
      this.generateWithMockData();       // mock
    }
  }

  openPreview(): void {
    // Fait défiler jusqu'à l'aperçu ou ouvre une modale
    const previewElement = document.querySelector('app-block-preview');
    if (previewElement) {
      previewElement.scrollIntoView({ behavior: 'smooth' });
    }
  }


  loadSchema(): void{
    if(!this.template) return;
    this.api.getSchema(this.template.id).subscribe({
      next: (schema) => {
        this.schema = schema;
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error("Erreur lor du chargement", err);
        this.schema = null;
        this.cdr.markForCheck();
      }
    });
  }

  loadTemplate(id?: string): void {
    const templateId = id || this.route.snapshot.paramMap.get('id');
    if (!templateId) return;

    this.templateId = templateId;
    this.loading = true;
    this.error = '';
    this.template = null;
    this.blocks = [];           // réinitialise la liste des blocs

    this.api.getTemplate(templateId).subscribe({
      next: (template) => {
        this.template = template;
        this.loading = false;

        // Charge le design dans le designer
        this.loadDesign();

        if (template.statut === 'PUBLIE') {
          this.loadSchema();
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = 'Impossible de charger le modèle.';
        this.loading = false;
        console.error(err);
        this.cdr.markForCheck();
      }
    });
  }

  loadVariables(templateId: string): void {
    this.api.getVariables(templateId).subscribe({
      next: (variables) => {
        this.variables = variables;
        this.cdr.markForCheck();
      }, 
      error: (err) => {
        console.error(err);
        this.cdr.markForCheck();
      }
    })
  }

  publish(): void {
    if (!this.template || this.publishing) return;
    this.publishing = true;
    this.api.publishTemplate(this.template.id).subscribe({
      next: (updated) => {
        this.template = updated;
        this.loadSchema();
        this.publishing = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        // Gère l'erreur silencieusement (le statut est peut-être déjà à PUBLIE)
        console.warn('Publication échouée, rechargement du template...');
        this.loadTemplate(this.template!.id); // recharge tout pour avoir le vrai état
        this.publishing = false;
        this.cdr.markForCheck();
      }
    });
  }

  addVariable(): void {
    if (!this.template || this.variableForm.invalid) return;
    const formVal = this.variableForm.value;
    const newVar = {
      nomVariable: formVal.nomVariable,
      type: formVal.type,
      obligatoire: formVal.obligatoire
    };
    this.api.addVariable(this.template.id, newVar).subscribe({
      next: () => {
        this.loadVariables(this.template!.id);
        this.variableForm.reset({ type: 'STRING', obligatoire: false });
      },
      error: (err) => {
        alert('Erreur lors de l\'ajout de la variable (peut-être un doublon ou template publié).');
        console.error(err);
      }
    });
  }

  deleteVariable(variableId: string): void {
    if (!this.template) return;
    this.api.deleteVariable(this.template.id, variableId).subscribe({
      next: () => this.loadVariables(this.template!.id),
      error: (err) => console.error(err)
    });
  }

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
        this.handleGenerationError(err);
      }
      console.error(err);
      this.cdr.markForCheck();
    }
    this.cdr.detectChanges();
  }

  triggerSave(): void {
    if (!this.template || this.template.statut !== 'BROUILLON') return;
    console.log('triggerSave called, current status:', this.savingStatus);
    if (this.savingStatus === 'idle' || this.savingStatus === 'saved') {
      this.savingStatus = 'saving';
      this.cdr.detectChanges();
    }
    this.saveSubject.next();
  }

  private doSave(): void {
    console.log('doSave exécuté');
    if (!this.template) {
      this.savingStatus = 'idle';
      return;
    }
    const contenuDesign = this.serializer.serialize(this.blocks);
    // On récupère les champs étendus depuis le template actuel
    const updateData: any = {
      nom: this.template.nom,
      description: this.template.description,
      contenuDesign: contenuDesign,
      categorie: (this.template as any).categorie || 'AUTRES',
      formatPapier: (this.template as any).formatPapier || 'A4'
    };
    this.api.updateTemplate(this.template.id, updateData).subscribe({
      next: (updated) => {
        this.template = updated;
        this.savingStatus = 'saved';
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur sauvegarde design', err);
        // Affiche le corps de l'erreur serveur
        if (err.error) {
          console.log('Détail erreur serveur :', err.error);
        }
        this.savingStatus = 'idle';
        this.cdr.detectChanges();
      }
    });
  }

  onBlocksChange(newBlocks: DesignBlock[]): void {
    this.blocks = newBlocks;
    this.triggerSave();
  }

}
