import { Component, OnInit, ChangeDetectorRef, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Template } from '../../models/template.model';
import { TemplateApiService } from '../../services/template-api';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { ReportDesigner } from '../../designer/report-designer/report-designer';
import { DesignBlock, DesignPage } from '../../designer/models/design-block.model';
import { DesignSerializer } from '../../designer/services/design-serializer.service';
import { Subject, debounceTime } from 'rxjs';
import { MockDataService } from '../../designer/services/mock-data.service';
import { VariableValidationForm } from '../../designer/variable-validation-form/variable-validation-form';
import { LucideAngularModule, Archive, RefreshCw, RotateCcw } from 'lucide-angular';
import { TemplateFiller } from '../../designer/template-filler/template-filler'; // ajuste le chemin

@Component({
  selector: 'app-template-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, ReportDesigner, VariableValidationForm, LucideAngularModule, TemplateFiller],
  templateUrl: './template-detail.html',
  styleUrls: ['./template-detail.scss'],
})
export class TemplateDetail implements OnInit {
  template: Template | null = null;
  generationError: string = '';
  loading = false;
  error = '';
  publishing = false;
  pages: DesignPage[] = [];
  savingStatus: 'idle' | 'saving' | 'saved' = 'idle';
  private saveSubject = new Subject<void>();
  templateId: string | null = null;
  selectedBlock: DesignBlock | null = null;
   modePagination: 'FIXED' | 'AUTO' = 'FIXED';

  readonly icons = {
    archive: Archive,
    newVersion: RefreshCw,
    restore: RotateCcw
  };

  constructor(
    private api: TemplateApiService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private serializer: DesignSerializer,
    private mockDataService: MockDataService,
  ) {}

  ngOnInit(): void {
    this.loadTemplate();
    this.saveSubject.pipe(debounceTime(2000)).subscribe(() => this.doSave());
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['templateId']) {
      console.log('templateId reçu :', this.templateId);
    }
  }

  get allBlocksFlat() {
    return this.pages.flatMap(p => p.blocks);
  }

  generateWithMockData(dataOverride?: any): void {
    if (!this.template) return;
    if (this.template.statut !== 'PUBLIE') {
      this.generationError = 'Le template doit être publié avant de pouvoir générer un document.';
      this.cdr.detectChanges();
      return;
    }

    this.api.getVariables(this.template.id).subscribe({
      next: (variables) => {
        const defaultData: Record<string, any> = {};
        for (const v of variables) {
          switch (v.type) {
            case 'ARRAY':
              defaultData[v.nomVariable] = [];
              break;
            case 'FLOAT':
              defaultData[v.nomVariable] = 0;
              break;
            case 'BOOLEAN':
              defaultData[v.nomVariable] = false;
              break;
            case 'IMAGE':
              defaultData[v.nomVariable] = '';
              break;
            default:
              defaultData[v.nomVariable] = '';
          }
        }

        const mockData = this.mockDataService.generate(this.allBlocksFlat);
        const data = { ...defaultData, ...(dataOverride || mockData) };

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
          error: (err) => this.handleGenerationError(err),
        });
      },
      error: (err) => {
        this.generationError = 'Impossible de charger les variables du template.';
        console.error(err);
      },
    });
  }

  duplicateAndNavigate(): void {
    if (!this.template) return;
    this.api.duplicateTemplate(this.template.id).subscribe({
      next: (copy) => this.router.navigate(['/templates', copy.id]),
      error: (err) => {
        alert('Échec de la duplication.');
        console.error(err);
      },
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
      this.generationError =
        'Erreur lors de la génération. Vérifiez les données et que le template est publié.';
    }
    console.error(err);
  }

  onExportRequest(data?: any): void {
    this.generateWithMockData(data);
  }

  openPreview(): void {
    const previewElement = document.querySelector('app-block-preview');
    if (previewElement) {
      previewElement.scrollIntoView({ behavior: 'smooth' });
    }
  }

  loadTemplate(id?: string): void {
    const templateId = id || this.route.snapshot.paramMap.get('id');
    if (!templateId) return;

    this.templateId = templateId;
    this.loading = true;
    this.error = '';
    this.template = null;
    this.pages = [];
    this.api.getTemplate(templateId).subscribe({
      next: (template) => {
        this.template = template;
        this.modePagination = template.modePagination || 'FIXED'
        this.loading = false;
        this.loadDesign();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = 'Impossible de charger le modèle.';
        this.loading = false;
        console.error(err);
        this.cdr.detectChanges();
      },
    });
  }

  publish(): void {
    if (!this.template || this.publishing) return;
      const contenuDesign = this.serializer.serialize(this.pages);
      const updateData: any = {
        nom: this.template.nom,
        description: this.template.description,
        contenuDesign: contenuDesign,
        categorie: (this.template as any).categorie || 'AUTRES',
        formatPapier: (this.template as any).formatPapier || 'A4',
        modePagination: this.template.modePagination || 'FIXED',   // ← ajouté
      };
      if (this.template.formatPapier === 'CUSTOM') {
        updateData.largeurMm = this.template.largeurMm;
        updateData.hauteurMm = this.template.hauteurMm;
      }
      this.api.updateTemplate(this.template.id, updateData).subscribe({
      next: () => {
        this.publishing = true;
        this.api.publishTemplate(this.template!.id).subscribe({
          next: (updated) => {
            this.template = updated;
            this.publishing = false;
            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error('Échec publication', err);
            this.publishing = false;
          },
        });
      },
      error: (err) => {
        alert('Impossible de sauvegarder le design avant publication.');
        console.error(err);
      },
    });
  }

  saveNow(): void {
    this.doSave();
  }

  loadDesign(): void {
    this.pages = this.template?.contenuDesign
      ? this.serializer.deserialize(this.template.contenuDesign)
      : [this.serializer.createEmptyPage('Page 1')];
    this.cdr.detectChanges();
  }

  triggerSave(): void {
    if (!this.template || this.template.statut !== 'BROUILLON') return;
    if (this.savingStatus === 'idle' || this.savingStatus === 'saved') {
      this.savingStatus = 'saving';
      this.cdr.detectChanges();
    }
    this.saveSubject.next();
  }

  private doSave(): void {
    if (!this.template) {
      this.savingStatus = 'idle';
      return;
    }
    const contenuDesign = this.serializer.serialize(this.pages);
    const updateData: any = {
      nom: this.template.nom,
      description: this.template.description,
      contenuDesign: contenuDesign,
      categorie: (this.template as any).categorie || 'AUTRES',
      formatPapier: (this.template as any).formatPapier || 'A4',
      modePagination: this.template.modePagination || 'FIXED',
      margeHautMm: this.template.margeHautMm ?? 10,
      margeBasMm: this.template.margeBasMm ?? 10,
      margeGaucheMm: this.template.margeGaucheMm ?? 10,
      margeDroiteMm: this.template.margeDroiteMm ?? 10,
    };
    if (this.template.formatPapier === 'CUSTOM') {
      updateData.largeurMm = this.template.largeurMm;
      updateData.hauteurMm = this.template.hauteurMm;
    }
    this.api.updateTemplate(this.template.id, updateData).subscribe({
      next: (updated) => {
        this.template = updated;
        this.savingStatus = 'saved';
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur sauvegarde design', err);
        this.savingStatus = 'idle';
        this.cdr.detectChanges();
      },
    });
  }

  onPagesChange(newPages: DesignPage[]): void {
    this.pages = newPages;
    this.triggerSave();
  }

  archive(): void {
    if (!this.template) return;
    if (!confirm('Archiver ce modèle ?')) return;
    this.api.archiveTemplate(this.template.id).subscribe({
      next: (updated) => {
        this.template = updated;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erreur archivage', err)
    });
  }

  newVersion(): void {
    if (!this.template) return;
    if (!confirm('Créer une nouvelle version ? L\'ancienne sera archivée.')) return;
    this.api.newVersion(this.template.id).subscribe({
      next: (newTemplate) => this.router.navigate(['/templates', newTemplate.id]),
      error: (err) => console.error('Erreur nouvelle version', err)
    });
  }

  restore(): void {
    if (!this.template) return;
    if (!confirm('Restaurer ce modèle ? Il repassera en brouillon.')) return;
    this.api.restoreTemplate(this.template.id).subscribe({
      next: (updated) => {
        this.template = updated;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erreur restauration', err)
    });
  }
}