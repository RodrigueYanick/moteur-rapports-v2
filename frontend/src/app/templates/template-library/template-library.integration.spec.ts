import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { TemplateLibrary } from './template-library';
import { TemplateApiService } from '../../services/template-api';
import { Template } from '../../models/template.model';

describe('TemplateLibrary - Tests d\'Intégration Complets', () => {
  let fixture: ComponentFixture<TemplateLibrary>;
  let component: TemplateLibrary;
  let httpTesting: HttpTestingController;
  let router: Router;

  const mockTemplates: Template[] = [
    {
      id: 'tpl-facture',
      nom: 'Facture Standard Pro',
      description: 'Facture avec TVA et totaux',
      statut: 'PUBLIE',
      version: 2,
      categorie: 'VENTES',
      formatPapier: 'A4',
      modePagination: 'FIXED',
      dateCreation: '2026-01-10T10:00:00Z',
      dateModification: '2026-02-15T14:30:00Z',
    },
    {
      id: 'tpl-paie',
      nom: 'Bulletin de Paie Cadre',
      description: 'Fiche de paie mensuelle',
      statut: 'BROUILLON',
      version: 1,
      categorie: 'RH',
      formatPapier: 'A4',
      modePagination: 'AUTO',
      dateCreation: '2026-03-01T08:00:00Z',
      dateModification: '2026-03-02T09:00:00Z',
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TemplateLibrary],
      providers: [
        TemplateApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TemplateLibrary);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  describe('1. Rendu initial et cycle de vie', () => {
    it('devrait afficher l\'état de chargement puis afficher la grille des modèles récupérés depuis l\'API', async () => {
      // Déclenche ngOnInit -> loadTemplates()
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;

      // Pendant la requête HTTP, l'état de chargement est visible
      const loadingEl = compiled.querySelector('.state-msg');
      expect(loadingEl?.textContent).toContain('Chargement des modèles...');

      // Réception de l'appel HTTP initial
      const req = httpTesting.expectOne((r) => r.url === '/api/templates');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('visibilite')).toBe('ALL');

      // Réponse du backend
      req.flush(mockTemplates);

      fixture.detectChanges();
      await fixture.whenStable();

      // Vérification que le spinner a disparu
      expect(compiled.querySelector('.state-msg')).toBeNull();

      // Vérification du compteur de modèles
      const countEl = compiled.querySelector('.template-count');
      expect(countEl?.textContent?.trim()).toBe('2 modèles');

      // Vérification du rendu des cartes dans le DOM
      const cards = compiled.querySelectorAll('.cards-grid .card.hover-lift');
      expect(cards.length).toBe(2);

      // Titres des cartes
      const titles = Array.from(cards).map((c) => c.querySelector('h3')?.textContent?.trim());
      expect(titles).toContain('Facture Standard Pro');
      expect(titles).toContain('Bulletin de Paie Cadre');

      // Badges de statut
      const badges = Array.from(cards).map((c) => c.querySelector('.badge')?.textContent?.trim());
      expect(badges).toContain('PUBLIE');
      expect(badges).toContain('BROUILLON');
    });
  });

  describe('2. Interactions du DOM (Happy Path)', () => {
    beforeEach(async () => {
      // Initialise le composant avec les modèles
      fixture.detectChanges();
      const req = httpTesting.expectOne((r) => r.url === '/api/templates');
      req.flush(mockTemplates);
      fixture.detectChanges();
      await fixture.whenStable();
    });

    it('devrait basculer entre la vue grille et la vue liste via les boutons de toggle', async () => {
      const compiled = fixture.nativeElement as HTMLElement;

      // Par défaut, la grille est affichée
      expect(compiled.querySelector('.cards-grid')).not.toBeNull();
      expect(compiled.querySelector('.table-wrapper')).toBeNull();

      // Clic sur le bouton de vue liste
      const listBtn = compiled.querySelector<HTMLButtonElement>('.view-toggle button[title="Vue liste"]')!;
      listBtn.click();
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.viewMode).toBe('list');
      expect(compiled.querySelector('.table-wrapper')).not.toBeNull();
      expect(compiled.querySelectorAll('.table tbody tr').length).toBe(2);

      // Retour en vue grille
      const gridBtn = compiled.querySelector<HTMLButtonElement>('.view-toggle button[title="Vue grille"]')!;
      gridBtn.click();
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.viewMode).toBe('grid');
      expect(compiled.querySelector('.cards-grid')).not.toBeNull();
    });

    it('devrait rechercher des modèles dynamiquement et mettre à jour le DOM avec le résultat de l\'API', async () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const searchInput = compiled.querySelector<HTMLInputElement>('.search-box input')!;

      // Saisie dans le champ de recherche - ngModelChange déclenche automatiquement onSearchChange
      searchInput.value = 'Facture';
      searchInput.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      await fixture.whenStable();

      // Nouvelle requête HTTP avec paramètre de recherche
      const req = httpTesting.expectOne((r) => r.url === '/api/templates' && r.params.get('q') === 'Facture');
      expect(req.request.method).toBe('GET');

      req.flush([mockTemplates[0]]); // renvoie uniquement la facture

      fixture.detectChanges();
      await fixture.whenStable();

      const cards = compiled.querySelectorAll('.cards-grid .card.hover-lift');
      expect(cards.length).toBe(1);
      expect(cards[0].querySelector('h3')?.textContent?.trim()).toBe('Facture Standard Pro');
    });

    it('devrait dupliquer un modèle au clic sur le bouton Dupliquer et recharger la liste', async () => {
      const compiled = fixture.nativeElement as HTMLElement;

      // Bouton dupliquer sur la première carte
      const dupBtn = compiled.querySelector<HTMLButtonElement>('.card .hover-btn.ghost')!;
      dupBtn.click();
      fixture.detectChanges();

      // Requête de duplication
      const dupReq = httpTesting.expectOne('/api/templates/tpl-facture/duplicate');
      expect(dupReq.request.method).toBe('POST');
      dupReq.flush({
        ...mockTemplates[0],
        id: 'tpl-facture-copy',
        nom: 'Facture Standard Pro (Copie)',
      });

      // Requête de rechargement automatique
      const reloadReq = httpTesting.expectOne((r) => r.url === '/api/templates');
      reloadReq.flush([
        ...mockTemplates,
        {
          ...mockTemplates[0],
          id: 'tpl-facture-copy',
          nom: 'Facture Standard Pro (Copie)',
        },
      ]);

      fixture.detectChanges();
      await fixture.whenStable();

      const cards = compiled.querySelectorAll('.cards-grid .card.hover-lift');
      expect(cards.length).toBe(3);
    });
  });

  describe('3. Gestion des formulaires et cas aux limites', () => {
    it('devrait afficher l\'état vide convivial quand aucun modèle ne correspond ou n\'est retourné', async () => {
      fixture.detectChanges();
      const req = httpTesting.expectOne((r) => r.url === '/api/templates');
      req.flush([]); // Aucun template retourné

      fixture.detectChanges();
      await fixture.whenStable();

      const compiled = fixture.nativeElement as HTMLElement;

      // Vérifie l'état vide
      const emptyState = compiled.querySelector('.empty-state-box');
      expect(emptyState).not.toBeNull();
      expect(emptyState?.textContent).toContain('Aucun modèle trouvé');
      expect(emptyState?.textContent).toContain('Commencez par concevoir votre premier modèle');

      // Le bouton "Nouveau modèle" est présent dans l'état vide
      const newBtn = emptyState?.querySelector('button');
      expect(newBtn?.textContent).toContain('Nouveau modèle');
    });

    it('devrait filtrer localement les modèles selon la catégorie sélectionnée', async () => {
      fixture.detectChanges();
      const req = httpTesting.expectOne((r) => r.url === '/api/templates');
      req.flush(mockTemplates);
      fixture.detectChanges();
      await fixture.whenStable();

      const compiled = fixture.nativeElement as HTMLElement;

      // Sélectionner catégorie RH
      component.filterCategorie = 'RH';
      component.applyFilters();
      fixture.detectChanges();
      await fixture.whenStable();

      const cards = compiled.querySelectorAll('.cards-grid .card.hover-lift');
      expect(cards.length).toBe(1);
      expect(cards[0].querySelector('h3')?.textContent?.trim()).toBe('Bulletin de Paie Cadre');
    });
  });

  describe('4. Gestion des erreurs asynchrones', () => {
    it('devrait afficher un message d\'erreur dans le DOM si l\'API GET /api/templates renvoie une erreur 500', async () => {
      fixture.detectChanges();

      const req = httpTesting.expectOne((r) => r.url === '/api/templates');
      // Erreur serveur 500
      req.flush(null, { status: 500, statusText: 'Internal Server Error' });

      fixture.detectChanges();
      await fixture.whenStable();

      const compiled = fixture.nativeElement as HTMLElement;

      // L'état de chargement a disparu
      expect(component.loading).toBe(false);

      // Le message d'erreur est affiché à l'écran
      const errorEl = compiled.querySelector('.state-msg.error');
      expect(errorEl).not.toBeNull();
      expect(errorEl?.textContent?.trim()).toBe('Erreur lors du chargement des modèles.');

      // Aucune carte n'est affichée
      const cards = compiled.querySelectorAll('.cards-grid .card');
      expect(cards.length).toBe(0);
    });
  });
});
