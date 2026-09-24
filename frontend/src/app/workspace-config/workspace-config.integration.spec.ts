import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { WorkspaceConfigComponent } from './workspace-config';
import { TemplateApiService } from '../services/template-api';
import { WorkspaceConfig } from '../models/workspace-config.model';

describe('WorkspaceConfigComponent - Tests d\'Intégration Complets', () => {
  let fixture: ComponentFixture<WorkspaceConfigComponent>;
  let component: WorkspaceConfigComponent;
  let httpTesting: HttpTestingController;

  const mockConfig: WorkspaceConfig = {
    codeEntreprise: 'ENT-TEST',
    formatPapier: 'A4',
    largeurMm: 210,
    hauteurMm: 297,
    modePagination: 'FIXED',
    margeGaucheMm: 15,
    margeDroiteMm: 15,
    margeHautMm: 20,
    margeBasMm: 20,
    couleurFond: '#ffffff',
    headerActif: true,
    hauteurHeaderMm: 18,
    headerContenu: 'En-tête entreprise',
    headerAlignement: 'GAUCHE',
    headerAfficherSurPremierePage: true,
    headerLigneSeparation: true,
    headerCouleurLigne: '#e2e8f0',
    footerActif: true,
    hauteurFooterMm: 14,
    footerContenu: 'Page {page} / {pages}',
    footerAlignement: 'CENTRE',
    footerAfficherSurPremierePage: true,
    footerLigneSeparation: true,
    footerCouleurLigne: '#e2e8f0',
    numerotationPage: true,
    formatNumerotation: 'PAGE_X_SUR_Y',
  };

  beforeEach(async () => {
    localStorage.clear();
    localStorage.setItem('entrepriseCode', 'ENT-TEST');

    await TestBed.configureTestingModule({
      imports: [WorkspaceConfigComponent],
      providers: [
        TemplateApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(WorkspaceConfigComponent);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  describe('1. Rendu initial et cycle de vie', () => {
    it('devrait afficher l\'état de chargement puis charger et injecter la configuration dans le formulaire réactif', async () => {
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;

      // Affichage du loader
      const loadingEl = compiled.querySelector('.loading-state');
      expect(loadingEl).not.toBeNull();
      expect(loadingEl?.textContent).toContain('Chargement de la configuration de travail...');

      // Requête GET vers l'API
      const req = httpTesting.expectOne('/api/workspace-config');
      expect(req.request.method).toBe('GET');
      req.flush(mockConfig);

      fixture.detectChanges();
      await fixture.whenStable();

      // Le loader a disparu et la grille de configuration est affichée
      expect(compiled.querySelector('.loading-state')).toBeNull();
      expect(compiled.querySelector('.config-grid')).not.toBeNull();

      // Badge du code entreprise dans l'en-tête
      const badge = compiled.querySelector('.code-badge');
      expect(badge?.textContent).toContain('ENT-TEST');

      // Valeurs pré-remplies dans le formulaire réactif
      expect(component.form.get('formatPapier')?.value).toBe('A4');
      expect(component.form.get('margeGaucheMm')?.value).toBe(15);
      expect(component.form.get('margeHautMm')?.value).toBe(20);
      expect(component.form.get('headerActif')?.value).toBe(true);

      // Bouton d'enregistrement actif
      const saveBtn = compiled.querySelector<HTMLButtonElement>('.top-actions .btn-primary');
      expect(saveBtn?.disabled).toBe(false);
    });
  });

  describe('2. Interactions du DOM (Happy Path)', () => {
    beforeEach(async () => {
      fixture.detectChanges();
      httpTesting.expectOne('/api/workspace-config').flush(mockConfig);
      fixture.detectChanges();
      await fixture.whenStable();
    });

    it('devrait modifier les marges, soumettre le formulaire et afficher le bandeau de succès', async () => {
      const compiled = fixture.nativeElement as HTMLElement;

      const margeHautInput = compiled.querySelector<HTMLInputElement>('#margeHautMm')!;
      const saveBtn = compiled.querySelector<HTMLButtonElement>('.top-actions .btn-primary')!;

      // Modification de la marge haute à 25 mm
      margeHautInput.value = '25';
      margeHautInput.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.form.get('margeHautMm')?.value).toBe(25);

      // Clic sur "Enregistrer les modifications"
      saveBtn.click();
      fixture.detectChanges();

      // Interception du PUT /api/workspace-config
      const reqPut = httpTesting.expectOne('/api/workspace-config');
      expect(reqPut.request.method).toBe('PUT');
      expect(reqPut.request.body.margeHautMm).toBe(25);

      // Réponse de succès
      reqPut.flush({
        ...mockConfig,
        margeHautMm: 25,
      });

      fixture.detectChanges();
      await fixture.whenStable();

      // Affichage du bandeau de succès dans le DOM
      const successAlert = compiled.querySelector('.alert.alert-success');
      expect(successAlert).not.toBeNull();
      expect(successAlert?.textContent).toContain('Configuration enregistrée avec succès');
      expect(component.saving).toBe(false);
    });
  });

  describe('3. Gestion des formulaires et cas aux limites', () => {
    beforeEach(async () => {
      fixture.detectChanges();
      httpTesting.expectOne('/api/workspace-config').flush(mockConfig);
      fixture.detectChanges();
      await fixture.whenStable();
    });

    it('devrait désactiver le bouton d\'enregistrement lorsque des marges invalides (négatives) sont saisies', async () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const margeGaucheInput = compiled.querySelector<HTMLInputElement>('#margeGaucheMm')!;
      const saveBtn = compiled.querySelector<HTMLButtonElement>('.top-actions .btn-primary')!;

      // Saisie d'une valeur négative interdite par Validators.min(0)
      margeGaucheInput.value = '-10';
      margeGaucheInput.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.form.get('margeGaucheMm')?.invalid).toBe(true);
      expect(component.form.invalid).toBe(true);

      // Le bouton d'enregistrement doit être désactivé
      expect(saveBtn.disabled).toBe(true);

      // Tenter de cliquer ne doit pas déclencher d'appel PUT
      saveBtn.click();
      fixture.detectChanges();
      httpTesting.expectNone('/api/workspace-config');
    });

    it('devrait afficher dynamiquement les champs largeurMm et hauteurMm lors du passage en format CUSTOM', async () => {
      const compiled = fixture.nativeElement as HTMLElement;

      // Par défaut en A4, les champs de dimension libre ne sont pas affichés
      expect(compiled.querySelector('#largeurMm')).toBeNull();

      // Changement du format vers CUSTOM
      const formatSelect = compiled.querySelector<HTMLSelectElement>('#formatPapier')!;
      formatSelect.value = 'CUSTOM';
      formatSelect.dispatchEvent(new Event('change'));
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.form.get('formatPapier')?.value).toBe('CUSTOM');

      // Les inputs personnalisés doivent maintenant être présents dans le DOM
      const largeurInput = compiled.querySelector<HTMLInputElement>('#largeurMm');
      const hauteurInput = compiled.querySelector<HTMLInputElement>('#hauteurMm');

      expect(largeurInput).not.toBeNull();
      expect(hauteurInput).not.toBeNull();
    });
  });

  describe('4. Gestion des erreurs asynchrones', () => {
    beforeEach(async () => {
      fixture.detectChanges();
      httpTesting.expectOne('/api/workspace-config').flush(mockConfig);
      fixture.detectChanges();
      await fixture.whenStable();
    });

    it('devrait afficher le bandeau d\'alerte d\'erreur dans le DOM si l\'enregistrement échoue (500)', async () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const saveBtn = compiled.querySelector<HTMLButtonElement>('.top-actions .btn-primary')!;

      saveBtn.click();
      fixture.detectChanges();

      const req = httpTesting.expectOne('/api/workspace-config');
      req.flush({ message: 'Base de données temporairement inaccessible' }, { status: 500, statusText: 'Internal Server Error' });

      fixture.detectChanges();
      await fixture.whenStable();

      // Vérification de la réaction de l'interface
      expect(component.saving).toBe(false);

      const dangerAlert = compiled.querySelector('.alert.alert-danger');
      expect(dangerAlert).not.toBeNull();
      expect(dangerAlert?.textContent).toContain('Erreur lors de la sauvegarde');

      // Le bouton redevient disponible pour que l'utilisateur puisse réitérer l'action
      expect(saveBtn.disabled).toBe(false);
    });
  });
});
