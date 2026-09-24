import { Component, EventEmitter, Input, Output, OnInit, OnChanges, SimpleChanges, inject, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReportDesigner } from '../designer/report-designer/report-designer';
import { TemplateApiService } from '../services/template-api';
import { AuthService } from '../services/auth.service';
import { DesignPage } from '../designer/models/design-block.model';
import { Template } from '../models/template.model';

@Component({
  selector: 'app-report-designer-widget',
  standalone: true,
  imports: [CommonModule, ReportDesigner],
  template: `
    <div class="report-designer-widget-container" [attr.data-theme]="theme">
      @if (loading) {
        <div class="widget-loader">
          <div class="spinner"></div>
          <p>Chargement du modèle de rapport...</p>
        </div>
      } @else if (errorMessage) {
        <div class="widget-error">
          <p class="error-text">{{ errorMessage }}</p>
          <button (click)="retryLoad()" class="btn-retry">Réessayer</button>
        </div>
      } @else {
        <app-report-designer
          [templateId]="templateId"
          [templateName]="template?.nom || 'Nouveau Rapport'"
          [pages]="pages"
          [formatPapier]="template?.formatPapier || 'A4'"
          [largeurMm]="template?.largeurMm"
          [hauteurMm]="template?.hauteurMm"
          [modePagination]="template?.modePagination || 'FIXED'"
          [margeHautMm]="template?.margeHautMm || 10"
          [margeBasMm]="template?.margeBasMm || 10"
          [margeGaucheMm]="template?.margeGaucheMm || 10"
          [margeDroiteMm]="template?.margeDroiteMm || 10"
          (save)="onSave($event)"
          (publish)="onSave($event)"
          (close)="onClose()"
        ></app-report-designer>
      }
    </div>
  `,
  styles: [`
    :host {
      display: block;
      width: 100%;
      height: 100%;
      min-height: 600px;
    }
    .report-designer-widget-container {
      width: 100%;
      height: 100%;
      position: relative;
      background: var(--bg-primary, #ffffff);
      color: var(--text-primary, #1e293b);
    }
    .widget-loader, .widget-error {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      min-height: 400px;
      gap: 1rem;
    }
    .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid rgba(59, 130, 246, 0.2);
      border-top-color: #3b82f6;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    .error-text {
      color: #ef4444;
      font-weight: 500;
    }
    .btn-retry {
      padding: 0.5rem 1rem;
      background: #3b82f6;
      color: white;
      border: none;
      border-radius: 0.375rem;
      cursor: pointer;
    }
  `],
  encapsulation: ViewEncapsulation.Emulated
})
export class ReportDesignerWidgetComponent implements OnInit, OnChanges {
  @Input('template-id') templateId = '';
  @Input('api-base-url') apiBaseUrl = '';
  @Input('auth-token') authToken = '';
  @Input('theme') theme: 'light' | 'dark' | 'auto' = 'auto';

  @Output('save') save = new EventEmitter<any>();
  @Output('close') close = new EventEmitter<void>();
  @Output('error') error = new EventEmitter<any>();

  private templateApi = inject(TemplateApiService);
  private authService = inject(AuthService);

  loading = false;
  errorMessage: string | null = null;
  template: Template | null = null;
  pages: DesignPage[] = [];

  ngOnInit(): void {
    this.applyAuthAndTheme();
    if (this.templateId) {
      this.loadTemplate();
    } else {
      this.initDefaultPages();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['authToken'] || changes['apiBaseUrl'] || changes['theme']) {
      this.applyAuthAndTheme();
    }
    if (changes['templateId'] && !changes['templateId'].firstChange) {
      this.loadTemplate();
    }
  }

  private applyAuthAndTheme(): void {
    if (this.authToken) {
      localStorage.setItem('auth_token', this.authToken);
    }
  }

  loadTemplate(): void {
    if (!this.templateId) return;

    this.loading = true;
    this.errorMessage = null;

    this.templateApi.getTemplate(this.templateId).subscribe({
      next: (tpl) => {
        this.template = tpl;
        if (tpl.contenuDesign) {
          try {
            const parsed = JSON.parse(tpl.contenuDesign);
            this.pages = Array.isArray(parsed) ? parsed : (parsed.pages || []);
          } catch (e) {
            console.warn('Erreur de parsing contenuDesign widget:', e);
            this.initDefaultPages();
          }
        } else {
          this.initDefaultPages();
        }
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = 'Impossible de charger le modèle : ' + (err.message || err.statusText || 'Erreur inconnue');
        this.error.emit(err);
      }
    });
  }

  private initDefaultPages(): void {
    this.pages = [{
      id: 'page-1',
      nom: 'Page 1',
      blocks: []
    }];
  }

  retryLoad(): void {
    if (this.templateId) {
      this.loadTemplate();
    }
  }

  onSave(eventData: any): void {
    this.save.emit(eventData);
  }

  onClose(): void {
    this.close.emit();
  }
}

