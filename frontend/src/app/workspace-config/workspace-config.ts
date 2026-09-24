import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TemplateApiService } from '../services/template-api';
import { WorkspaceConfig } from '../models/workspace-config.model';
import { catchError, finalize, timeout } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-workspace-config',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './workspace-config.html',
  styleUrls: ['./workspace-config.scss']
})
export class WorkspaceConfigComponent implements OnInit {
  form!: FormGroup;
  loading = true;
  saving = false;
  successMessage: string | null = null;
  errorMessage: string | null = null;
  codeEntreprise: string = 'DEFAULT';

  // Standard dimensions mm
  standardFormats: Record<string, { w: number; h: number }> = {
    A0: { w: 841, h: 1189 },
    A1: { w: 594, h: 841 },
    A2: { w: 420, h: 594 },
    A3: { w: 297, h: 420 },
    A4: { w: 210, h: 297 },
    A5: { w: 148, h: 210 },
    A6: { w: 105, h: 148 },
    Letter: { w: 216, h: 279 },
    Legal: { w: 216, h: 356 }
  };

  constructor(
    private fb: FormBuilder,
    private api: TemplateApiService,
    private cdr: ChangeDetectorRef
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.loadConfig();
  }

  private toUiAlign(align?: string | null): 'GAUCHE' | 'CENTRE' | 'DROITE' {
    if (!align) return 'GAUCHE';
    const up = align.toUpperCase().trim();
    if (up === 'CENTER' || up === 'CENTRE') return 'CENTRE';
    if (up === 'RIGHT' || up === 'DROITE') return 'DROITE';
    return 'GAUCHE';
  }

  private initForm(): void {
    this.form = this.fb.group({
      formatPapier: ['A4', Validators.required],
      largeurMm: [210],
      hauteurMm: [297],
      modePagination: ['FIXED', Validators.required],
      margeGaucheMm: [10, [Validators.required, Validators.min(0)]],
      margeDroiteMm: [10, [Validators.required, Validators.min(0)]],
      margeHautMm: [10, [Validators.required, Validators.min(0)]],
      margeBasMm: [10, [Validators.required, Validators.min(0)]],
      couleurFond: ['#ffffff', Validators.required],

      // En-tête
      headerActif: [false],
      hauteurHeaderMm: [15, [Validators.required, Validators.min(5)]],
      headerContenu: [''],
      headerAlignement: ['GAUCHE'],
      headerAfficherSurPremierePage: [true],
      headerLigneSeparation: [false],
      headerCouleurLigne: ['#cccccc'],

      // Pied de page
      footerActif: [false],
      hauteurFooterMm: [12, [Validators.required, Validators.min(5)]],
      footerContenu: ['Page {page} / {pages}'],
      footerAlignement: ['CENTRE'],
      footerAfficherSurPremierePage: [true],
      footerLigneSeparation: [false],
      footerCouleurLigne: ['#cccccc'],
      numerotationPage: [true],
      formatNumerotation: ['PAGE_X_SUR_Y']
    });

    this.form.get('formatPapier')?.valueChanges.subscribe(format => {
      const largeurCtrl = this.form.get('largeurMm');
      const hauteurCtrl = this.form.get('hauteurMm');
      if (format === 'CUSTOM') {
        largeurCtrl?.setValidators([Validators.required, Validators.min(20)]);
        hauteurCtrl?.setValidators([Validators.required, Validators.min(20)]);
      } else {
        largeurCtrl?.clearValidators();
        hauteurCtrl?.clearValidators();
        const std = this.standardFormats[format] || this.standardFormats['A4'];
        largeurCtrl?.setValue(std.w, { emitEvent: false });
        hauteurCtrl?.setValue(std.h, { emitEvent: false });
      }
      largeurCtrl?.updateValueAndValidity();
      hauteurCtrl?.updateValueAndValidity();
      this.cdr.detectChanges();
    });

    this.form.valueChanges.subscribe(() => {
      this.cdr.detectChanges();
    });
  }

  private applyConfigToForm(config: WorkspaceConfig): void {
    const format = config.formatPapier || 'A4';
    const std = this.standardFormats[format] || this.standardFormats['A4'];
    const w = format === 'CUSTOM' ? (config.largeurMm || std.w) : std.w;
    const h = format === 'CUSTOM' ? (config.hauteurMm || std.h) : std.h;

    this.form.patchValue({
      formatPapier: format,
      largeurMm: w,
      hauteurMm: h,
      modePagination: config.modePagination || 'FIXED',
      margeGaucheMm: config.margeGaucheMm ?? 10,
      margeDroiteMm: config.margeDroiteMm ?? 10,
      margeHautMm: config.margeHautMm ?? 10,
      margeBasMm: config.margeBasMm ?? 10,
      couleurFond: config.couleurFond || '#ffffff',

      headerActif: config.headerActif ?? false,
      hauteurHeaderMm: config.hauteurHeaderMm ?? 15,
      headerContenu: config.headerContenu || '',
      headerAlignement: this.toUiAlign(config.headerAlignement),
      headerAfficherSurPremierePage: config.headerAfficherSurPremierePage ?? true,
      headerLigneSeparation: config.headerLigneSeparation ?? false,
      headerCouleurLigne: config.headerCouleurLigne || '#cccccc',

      footerActif: config.footerActif ?? false,
      hauteurFooterMm: config.hauteurFooterMm ?? 12,
      footerContenu: config.footerContenu || 'Page {page} / {pages}',
      footerAlignement: this.toUiAlign(config.footerAlignement || 'CENTRE'),
      footerAfficherSurPremierePage: config.footerAfficherSurPremierePage ?? true,
      footerLigneSeparation: config.footerLigneSeparation ?? false,
      footerCouleurLigne: config.footerCouleurLigne || '#cccccc',
      numerotationPage: config.numerotationPage ?? true,
      formatNumerotation: config.formatNumerotation || 'PAGE_X_SUR_Y'
    }, { emitEvent: false });
  }

  loadConfig(): void {
    this.loading = true;
    this.errorMessage = null;
    this.cdr.detectChanges();

    this.api.getWorkspaceConfig().pipe(
      timeout(5000),
      catchError((err) => {
        console.warn('Impossible de charger la configuration distante, utilisation des valeurs par défaut.', err);
        return of({
          codeEntreprise: localStorage.getItem('entrepriseCode') || 'DEFAULT',
          formatPapier: 'A4',
          largeurMm: 210,
          hauteurMm: 297,
          modePagination: 'FIXED',
          margeGaucheMm: 10,
          margeDroiteMm: 10,
          margeHautMm: 10,
          margeBasMm: 10,
          couleurFond: '#ffffff',
          headerActif: false,
          hauteurHeaderMm: 15,
          headerContenu: '',
          headerAlignement: 'GAUCHE',
          headerAfficherSurPremierePage: true,
          headerLigneSeparation: false,
          headerCouleurLigne: '#cccccc',
          footerActif: false,
          hauteurFooterMm: 12,
          footerContenu: 'Page {page} / {pages}',
          footerAlignement: 'CENTRE',
          footerAfficherSurPremierePage: true,
          footerLigneSeparation: false,
          footerCouleurLigne: '#cccccc',
          numerotationPage: true,
          formatNumerotation: 'PAGE_X_SUR_Y'
        } as WorkspaceConfig);
      }),
      finalize(() => {
        this.loading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (config: WorkspaceConfig) => {
        this.codeEntreprise = config.codeEntreprise || localStorage.getItem('entrepriseCode') || 'DEFAULT';
        this.applyConfigToForm(config);
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  saveConfig(): void {
    if (this.form.invalid) return;

    this.saving = true;
    this.successMessage = null;
    this.errorMessage = null;
    this.cdr.detectChanges();

    const val = { ...this.form.value };

    this.api.updateWorkspaceConfig(val).pipe(
      finalize(() => {
        this.saving = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (res) => {
        this.successMessage = 'Configuration enregistrée avec succès ! Les nouveaux modèles utiliseront ces paramètres.';
        this.cdr.detectChanges();
        setTimeout(() => {
          this.successMessage = null;
          this.cdr.detectChanges();
        }, 5000);
      },
      error: (err) => {
        this.errorMessage = 'Erreur lors de la sauvegarde de la configuration.';
        console.error(err);
        this.cdr.detectChanges();
      }
    });
  }

  resetToDefaults(): void {
    if (!confirm('Voulez-vous réinitialiser tous les paramètres de la feuille de travail aux valeurs par défaut ?')) {
      return;
    }
    this.loading = true;
    this.successMessage = null;
    this.errorMessage = null;
    this.cdr.detectChanges();

    this.api.resetWorkspaceConfig().pipe(
      finalize(() => {
        this.loading = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: (res) => {
        this.applyConfigToForm(res);
        this.successMessage = 'Configuration réinitialisée aux valeurs par défaut.';
        this.cdr.detectChanges();
        setTimeout(() => {
          this.successMessage = null;
          this.cdr.detectChanges();
        }, 5000);
      },
      error: (err) => {
        this.errorMessage = 'Erreur lors de la réinitialisation.';
        console.error(err);
        this.cdr.detectChanges();
      }
    });
  }

  insertToken(field: 'headerContenu' | 'footerContenu', token: string): void {
    const current = this.form.get(field)?.value || '';
    this.form.get(field)?.setValue(current + (current ? ' ' : '') + token);
    this.form.get(field)?.markAsDirty();
    this.cdr.detectChanges();
  }

  // --- Dynamic calculations for live preview ---

  get previewWidthMm(): number {
    const format = this.form.get('formatPapier')?.value;
    if (format === 'CUSTOM') {
      return this.form.get('largeurMm')?.value || 210;
    }
    return this.standardFormats[format]?.w || 210;
  }

  get previewHeightMm(): number {
    const format = this.form.get('formatPapier')?.value;
    if (format === 'CUSTOM') {
      return this.form.get('hauteurMm')?.value || 297;
    }
    return this.standardFormats[format]?.h || 297;
  }

  get mLeft(): number { return this.form.get('margeGaucheMm')?.value || 0; }
  get mRight(): number { return this.form.get('margeDroiteMm')?.value || 0; }
  get mTop(): number { return this.form.get('margeHautMm')?.value || 0; }
  get mBottom(): number { return this.form.get('margeBasMm')?.value || 0; }

  get headerActif(): boolean { return this.form.get('headerActif')?.value; }
  get headerHeight(): number { return this.headerActif ? (this.form.get('hauteurHeaderMm')?.value || 0) : 0; }

  get footerActif(): boolean { return this.form.get('footerActif')?.value; }
  get footerHeight(): number { return this.footerActif ? (this.form.get('hauteurFooterMm')?.value || 0) : 0; }

  get usableWidthMm(): number {
    return Math.max(0, this.previewWidthMm - this.mLeft - this.mRight);
  }

  get usableHeightMm(): number {
    return Math.max(0, this.previewHeightMm - this.mTop - this.headerHeight - this.footerHeight - this.mBottom);
  }

  get usableSurfacePercent(): number {
    const totalArea = this.previewWidthMm * this.previewHeightMm;
    const usableArea = this.usableWidthMm * this.usableHeightMm;
    if (totalArea <= 0) return 0;
    return Math.round((usableArea / totalArea) * 100);
  }

  get isDimensionsInvalid(): boolean {
    return this.usableWidthMm <= 0 || this.usableHeightMm <= 0;
  }

  get formattedHeaderText(): string {
    const raw = this.form.get('headerContenu')?.value || 'Titre du rapport | {date}';
    return this.resolveTokens(raw, 1, 3);
  }

  get formattedFooterText(): string {
    const raw = this.form.get('footerContenu')?.value || 'Page {page} / {pages}';
    return this.resolveTokens(raw, 1, 3);
  }

  private resolveTokens(text: string, page: number, total: number): string {
    const today = new Date().toLocaleDateString('fr-FR');
    return text
      .replace(/\{page\}/g, String(page))
      .replace(/\{pages\}/g, String(total))
      .replace(/\{date\}/g, today);
  }
}

