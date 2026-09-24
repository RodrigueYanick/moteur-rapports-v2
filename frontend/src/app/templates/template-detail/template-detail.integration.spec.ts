import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { TemplateDetail } from './template-detail';
import { TemplateApiService } from '../../services/template-api';
import { Template } from '../../models/template.model';

// Polyfills pour l'environnement jsdom
if (typeof globalThis.ResizeObserver === 'undefined') {
  globalThis.ResizeObserver = class ResizeObserver {
    observe(): void {}
    unobserve(): void {}
    disconnect(): void {}
  } as any;
}

if (!Element.prototype.scrollIntoView) {
  Element.prototype.scrollIntoView = vi.fn();
}

describe('TemplateDetail - Tests d\'Intégration Complets', () => {
  let fixture: ComponentFixture<TemplateDetail>;
  let component: TemplateDetail;
  let httpTesting: HttpTestingController;

  const mockTemplate: Template = {
    id: 'tpl-100',
    nom: 'Rapport Annuel Audit',
    description: 'Modèle officiel d\'audit interne',
    statut: 'BROUILLON',
    version: 1,
    categorie: 'FINANCE',
    formatPapier: 'A4',
    modePagination: 'FIXED',
    margeHautMm: 10,
    margeBasMm: 10,
    margeGaucheMm: 10,
    margeDroiteMm: 10,
    contenuDesign: JSON.stringify({
      pages: [
        {
          id: 'p1',
          nom: 'Page 1',
          blocks: [],
        },
      ],
    }),
    dateCreation: '2026-01-01T00:00:00Z',
    dateModification: '2026-01-02T00:00:00Z',
  };

  const mockVersionTree = {
    templateId: 'tpl-100',
    nom: 'Rapport Annuel Audit',
    versionActuelle: 1,
    statutActuel: 'BROUILLON',
    versions: [],
  };

  beforeEach(async () => {
    localStorage.clear();
    // Empêche le tour d'onboarding de se lancer pendant les tests
    localStorage.setItem('has_seen_designer_tour', 'true');
    localStorage.setItem('has_seen_tour', 'true');

    await TestBed.configureTestingModule({
      imports: [TemplateDetail],
      providers: [
        TemplateApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: 'tpl-100' }),
            },
            paramMap: of(convertToParamMap({ id: 'tpl-100' })),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TemplateDetail);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  describe('1. Rendu initial et cycle de vie', () => {
    it('devrait afficher l\'état de chargement puis charger et afficher le modèle et son en-tête', async () => {
      // Démarrage du cycle de vie ngOnInit
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;

      // Affichage du loader initial
      const loadingState = compiled.querySelector('.loading-state');
      expect(loadingState).not.toBeNull();
      expect(loadingState?.textContent).toContain('Chargement du modèle de rapport...');

      // Requêtes HTTP émises au démarrage
      const reqVersion = httpTesting.expectOne('/api/templates/tpl-100/versions');
      expect(reqVersion.request.method).toBe('GET');
      reqVersion.flush(mockVersionTree);

      const reqTemplate = httpTesting.expectOne('/api/templates/tpl-100');
      expect(reqTemplate.request.method).toBe('GET');
      reqTemplate.flush(mockTemplate);

      // Flusher les éventuelles requêtes émises par les sous-composants intégrés (ex: variables)
      const varRequests = httpTesting.match('/api/templates/tpl-100/variables');
      varRequests.forEach((req) => req.flush([]));

      fixture.detectChanges();
      await fixture.whenStable();

      // Le loader doit disparaître
      expect(compiled.querySelector('.loading-state')).toBeNull();

      // L'en-tête du studio doit s'afficher avec les données du modèle
      const headerTitle = compiled.querySelector('.detail-header h1');
      expect(headerTitle?.textContent?.trim()).toBe('Rapport Annuel Audit');

      const badge = compiled.querySelector('.detail-header .badge');
      expect(badge?.textContent?.trim()).toBe('BROUILLON');

      const versionTag = compiled.querySelector('.detail-header .version-tag');
      expect(versionTag?.textContent?.trim()).toBe('v1');

      // Le bouton "Publier" doit être présent pour un statut BROUILLON
      const buttons = Array.from(compiled.querySelectorAll('.actions-bar button'));
      const publishBtn = buttons.find((b) => b.textContent?.trim().includes('Publier'));
      expect(publishBtn).toBeDefined();
    });
  });

  describe('2. Interactions du DOM (Happy Path)', () => {
    beforeEach(async () => {
      fixture.detectChanges();
      httpTesting.expectOne('/api/templates/tpl-100/versions').flush(mockVersionTree);
      httpTesting.expectOne('/api/templates/tpl-100').flush(mockTemplate);
      const varRequests = httpTesting.match('/api/templates/tpl-100/variables');
      varRequests.forEach((req) => req.flush([]));
      fixture.detectChanges();
      await fixture.whenStable();
    });

    it('devrait publier le template au clic sur le bouton Publier et mettre à jour le DOM', async () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const buttons = Array.from(compiled.querySelectorAll<HTMLButtonElement>('.actions-bar button'));
      const publishBtn = buttons.find((b) => b.textContent?.trim().includes('Publier'))!;
      expect(publishBtn).toBeDefined();

      // Clic sur le bouton Publier
      publishBtn.click();
      fixture.detectChanges();

      // 1. Sauvegarde du contenu avant publication (PUT)
      const reqPut = httpTesting.expectOne('/api/templates/tpl-100');
      expect(reqPut.request.method).toBe('PUT');
      reqPut.flush(mockTemplate);

      // 2. Publication (POST /publish)
      const reqPublish = httpTesting.expectOne('/api/templates/tpl-100/publish');
      expect(reqPublish.request.method).toBe('POST');

      const publishedTemplate: Template = {
        ...mockTemplate,
        statut: 'PUBLIE',
      };
      reqPublish.flush(publishedTemplate);

      fixture.detectChanges();
      await fixture.whenStable();

      // Le statut dans le DOM doit maintenant être PUBLIE
      const badge = compiled.querySelector('.detail-header .badge');
      expect(badge?.textContent?.trim()).toBe('PUBLIE');

      // Le bouton "Publier" ne doit plus être présent
      const updatedButtons = Array.from(compiled.querySelectorAll('.actions-bar button'));
      expect(updatedButtons.some((b) => b.textContent?.trim().includes('Publier'))).toBe(false);
    });

    it('devrait basculer entre les onglets du studio (Designer, Versions, Test)', async () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const segmentButtons = compiled.querySelectorAll<HTMLButtonElement>('.segmented-control .segment-btn');
      expect(segmentButtons.length).toBe(3);

      // Par défaut tab = 'designer'
      expect(component.activeStudioTab).toBe('designer');
      expect(segmentButtons[0].classList.contains('active')).toBe(true);

      // Clic sur l'onglet Versions
      segmentButtons[1].click();
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.activeStudioTab).toBe('versions');
      expect(segmentButtons[1].classList.contains('active')).toBe(true);

      // Clic sur l'onglet Test
      segmentButtons[2].click();
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.activeStudioTab).toBe('test');
      expect(segmentButtons[2].classList.contains('active')).toBe(true);
    });
  });

  describe('3. Gestion des formulaires et cas aux limites', () => {
    it('devrait empêcher la génération de document et afficher une erreur si le modèle est encore un brouillon', async () => {
      fixture.detectChanges();
      httpTesting.expectOne('/api/templates/tpl-100/versions').flush(mockVersionTree);
      httpTesting.expectOne('/api/templates/tpl-100').flush(mockTemplate); // statut: 'BROUILLON'
      const varRequests = httpTesting.match('/api/templates/tpl-100/variables');
      varRequests.forEach((req) => req.flush([]));
      fixture.detectChanges();
      await fixture.whenStable();

      // Basculer sur l'onglet test pour voir le bandeau d'erreur de génération
      component.activeStudioTab = 'test';
      fixture.detectChanges();

      // Tentative de génération d'un brouillon
      component.generateWithMockData();
      fixture.detectChanges();
      await fixture.whenStable();

      // Aucun appel HTTP de génération de document ne doit être envoyé
      httpTesting.expectNone('/api/templates/tpl-100/generate');

      // Message d'erreur défini
      expect(component.generationError).toBe('Le template doit être publié avant de pouvoir générer un document.');

      // Le bandeau d'erreur s'affiche dans le DOM
      const compiled = fixture.nativeElement as HTMLElement;
      const alertEl = compiled.querySelector('.generation-error');
      expect(alertEl).not.toBeNull();
      expect(alertEl?.textContent).toContain('Le template doit être publié avant de pouvoir générer un document.');
    });
  });

  describe('4. Gestion des erreurs asynchrones', () => {
    it('devrait afficher un message d\'erreur dans le DOM si l\'API renvoie une 404 (template introuvable)', async () => {
      fixture.detectChanges();

      httpTesting.expectOne('/api/templates/tpl-100/versions').flush(null, { status: 404, statusText: 'Not Found' });
      const reqTemplate = httpTesting.expectOne('/api/templates/tpl-100');
      reqTemplate.flush({ message: 'Template non trouvé' }, { status: 404, statusText: 'Not Found' });

      fixture.detectChanges();
      await fixture.whenStable();

      const compiled = fixture.nativeElement as HTMLElement;

      // Le loader a disparu
      expect(compiled.querySelector('.loading-state')).toBeNull();

      // Le message d'erreur est affiché à l'écran
      const errorEl = compiled.querySelector('.message.error');
      expect(errorEl).not.toBeNull();
      expect(errorEl?.textContent?.trim()).toBe('Impossible de charger le modèle.');

      // L'en-tête du studio ne doit pas être affiché
      expect(compiled.querySelector('.studio-subbar')).toBeNull();
    });
  });
});

