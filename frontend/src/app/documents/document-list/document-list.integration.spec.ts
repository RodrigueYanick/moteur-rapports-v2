import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { DocumentList } from './document-list';
import { TemplateApiService } from '../../services/template-api';
import { GeneratedDocument } from '../../models/Document.model';
import { ToastService } from '../../shared/services/toast.service';

describe('DocumentList - Tests d\'Intégration Complets', () => {
  let fixture: ComponentFixture<DocumentList>;
  let component: DocumentList;
  let httpTesting: HttpTestingController;

  const mockDocuments: GeneratedDocument[] = [
    {
      id: 'doc-001',
      nom: 'Facture Client F-2026-001',
      templateId: 'tpl-facture',
      templateNom: 'Facture Standard Pro',
      dateCreation: '2026-02-01T10:00:00Z',
      dateModification: '2026-02-01T10:00:00Z',
      donnees: { montantTotal: 1500 },
    },
    {
      id: 'doc-002',
      nom: 'Bulletin de Paie - Février 2026',
      templateId: 'tpl-paie',
      templateNom: 'Bulletin de Paie Cadre',
      dateCreation: '2026-02-28T18:00:00Z',
      dateModification: '2026-02-28T18:00:00Z',
      donnees: { salaireNet: 3200 },
    },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DocumentList],
      providers: [
        TemplateApiService,
        ToastService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DocumentList);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  describe('1. Rendu initial et cycle de vie', () => {
    it('devrait afficher l\'état de chargement puis afficher la grille des documents générés', async () => {
      fixture.detectChanges();

      const compiled = fixture.nativeElement as HTMLElement;

      // État de chargement initial
      const loadingEl = compiled.querySelector('.state-msg');
      expect(loadingEl?.textContent).toContain('Chargement...');

      // Requête HTTP GET vers /api/documents
      const req = httpTesting.expectOne((r) => r.url === '/api/documents');
      expect(req.request.method).toBe('GET');
      req.flush(mockDocuments);

      fixture.detectChanges();
      await fixture.whenStable();

      // Le loader a disparu
      expect(compiled.querySelector('.state-msg')).toBeNull();

      // Les cartes de documents sont affichées dans la grille
      const cards = compiled.querySelectorAll('.cards-grid .card');
      expect(cards.length).toBe(2);

      const titles = Array.from(cards).map((c) => c.querySelector('h3')?.textContent?.trim());
      expect(titles).toContain('Facture Client F-2026-001');
      expect(titles).toContain('Bulletin de Paie - Février 2026');
    });
  });

  describe('2. Interactions du DOM (Happy Path)', () => {
    beforeEach(async () => {
      fixture.detectChanges();
      httpTesting.expectOne((r) => r.url === '/api/documents').flush(mockDocuments);
      fixture.detectChanges();
      await fixture.whenStable();
    });

    it('devrait basculer de la vue grille vers la vue liste au clic sur le bouton de vue', async () => {
      const compiled = fixture.nativeElement as HTMLElement;

      // Par défaut, la grille est présente
      expect(compiled.querySelector('.cards-grid')).not.toBeNull();
      expect(compiled.querySelector('.table-wrapper')).toBeNull();

      // Clic sur l'icône de vue liste
      const listBtn = compiled.querySelector<HTMLButtonElement>('.view-toggle button[title="Vue liste"]')!;
      listBtn.click();
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.viewMode).toBe('list');
      expect(compiled.querySelector('.table-wrapper')).not.toBeNull();
      expect(compiled.querySelectorAll('.table tbody tr').length).toBe(2);
    });

    it('devrait supprimer un document lors de la confirmation utilisateur et recharger la liste', async () => {
      vi.spyOn(window, 'confirm').mockReturnValue(true);

      const compiled = fixture.nativeElement as HTMLElement;
      const deleteBtn = compiled.querySelector<HTMLButtonElement>('.card .hover-btn.ghost')!;

      // Clic sur le bouton de suppression
      deleteBtn.click();
      fixture.detectChanges();

      // Interception de l'appel DELETE
      const reqDelete = httpTesting.expectOne('/api/templates/tpl-facture/documents/doc-001');
      expect(reqDelete.request.method).toBe('DELETE');
      reqDelete.flush(null);

      fixture.detectChanges();
      await fixture.whenStable();

      // Le document supprimé n'est plus présent dans le DOM (filtrage direct de la liste)
      const cards = compiled.querySelectorAll('.cards-grid .card');
      expect(cards.length).toBe(1);
      expect(cards[0].querySelector('h3')?.textContent?.trim()).toBe('Bulletin de Paie - Février 2026');
    });
  });

  describe('3. Gestion des formulaires et cas aux limites', () => {
    it('devrait filtrer localement les documents lors de la saisie d\'un terme de recherche', async () => {
      fixture.detectChanges();
      httpTesting.expectOne((r) => r.url === '/api/documents').flush(mockDocuments);
      fixture.detectChanges();
      await fixture.whenStable();

      const compiled = fixture.nativeElement as HTMLElement;

      // Saisie dans le champ de recherche
      component.searchTerm = 'Paie';
      fixture.detectChanges();
      await fixture.whenStable();

      const cards = compiled.querySelectorAll('.cards-grid .card');
      expect(cards.length).toBe(1);
      expect(cards[0].querySelector('h3')?.textContent?.trim()).toBe('Bulletin de Paie - Février 2026');
    });

    it('devrait afficher l\'état vide convivial lorsque la liste des documents est vide', async () => {
      fixture.detectChanges();
      httpTesting.expectOne((r) => r.url === '/api/documents').flush([]);
      fixture.detectChanges();
      await fixture.whenStable();

      const compiled = fixture.nativeElement as HTMLElement;

      const emptyBox = compiled.querySelector('.empty-state');
      expect(emptyBox).not.toBeNull();
      expect(emptyBox?.textContent).toContain('Aucun document sauvegardé pour le moment');
    });
  });

  describe('4. Gestion des erreurs asynchrones', () => {
    it('devrait afficher un message d\'erreur dans le DOM si l\'API documents renvoie une 500', async () => {
      fixture.detectChanges();

      const req = httpTesting.expectOne((r) => r.url === '/api/documents');
      req.flush(null, { status: 500, statusText: 'Internal Server Error' });

      fixture.detectChanges();
      await fixture.whenStable();

      const compiled = fixture.nativeElement as HTMLElement;

      expect(component.loading).toBe(false);
      const errorEl = compiled.querySelector('.state-msg.error');
      expect(errorEl).not.toBeNull();
      expect(errorEl?.textContent?.trim()).toBe('Erreur lors du chargement des documents.');
    });
  });
});
