import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { TemplateCreate } from './template-create';
import { TemplateApiService } from '../../services/template-api';
import { STARTER_TEMPLATES } from '../starter-templates/starter-templates.data';

describe('TemplateCreate - Tests d\'Intégration Complets', () => {
  let fixture: ComponentFixture<TemplateCreate>;
  let component: TemplateCreate;
  let httpTesting: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TemplateCreate],
      providers: [
        TemplateApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TemplateCreate);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);

    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => {
    httpTesting.verify();
  });

  describe('1. Rendu initial et cycle de vie', () => {
    it('devrait compiler le composant, initialiser le formulaire et afficher les starters avec Feuille vierge sélectionnée', () => {
      const compiled = fixture.nativeElement as HTMLElement;

      // Titre de la page
      const h1 = compiled.querySelector('h1');
      expect(h1?.textContent).toContain('Nouveau modèle de rapport');

      // Par défaut, 'blank' est sélectionné
      const blankCard = compiled.querySelector('.starter-card.blank-card');
      expect(blankCard).not.toBeNull();
      expect(blankCard?.classList.contains('selected')).toBe(true);

      // Le formulaire est initialisé avec nom vide
      expect(component.form.get('nom')?.value).toBe('');
      expect(component.form.get('categorie')?.value).toBe('AUTRES');
      expect(component.form.get('modePagination')?.value).toBe('FIXED');

      // Le bouton de soumission doit être désactivé car 'nom' est obligatoire
      const submitBtn = compiled.querySelector<HTMLButtonElement>('button[type="submit"]');
      expect(submitBtn).not.toBeNull();
      expect(submitBtn?.disabled).toBe(true);

      // Aucun message d'erreur présent initialement
      const errorMsg = compiled.querySelector('.error.message');
      expect(errorMsg).toBeNull();
    });
  });

  describe('2. Interactions du DOM (Happy Path)', () => {
    it('devrait remplir le formulaire avec un modèle vierge, soumettre et rediriger vers le designer', async () => {
      const navigateSpy = vi.spyOn(router, 'navigate');
      const compiled = fixture.nativeElement as HTMLElement;

      const nomInput = compiled.querySelector<HTMLInputElement>('#nom')!;
      const descInput = compiled.querySelector<HTMLTextAreaElement>('#description')!;
      const submitBtn = compiled.querySelector<HTMLButtonElement>('button[type="submit"]')!;

      // Saisie du nom
      nomInput.value = 'Rapport Financier Annuel';
      nomInput.dispatchEvent(new Event('input'));

      // Saisie de la description
      descInput.value = 'Bilan annuel d\'activité';
      descInput.dispatchEvent(new Event('input'));

      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.form.valid).toBe(true);
      expect(submitBtn.disabled).toBe(false);

      // Clic sur soumettre
      submitBtn.click();
      fixture.detectChanges();

      // Vérification de la requête HTTP POST /api/templates
      const req = httpTesting.expectOne('/api/templates');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        nom: 'Rapport Financier Annuel',
        description: 'Bilan annuel d\'activité',
        categorie: 'AUTRES',
        modePagination: 'FIXED',
      });

      // Réponse de création
      req.flush({
        id: 'new-tpl-123',
        nom: 'Rapport Financier Annuel',
        statut: 'BROUILLON',
        version: 1,
      });

      fixture.detectChanges();
      await fixture.whenStable();

      // Navigation vers /templates/new-tpl-123
      expect(navigateSpy).toHaveBeenCalledWith(['/templates', 'new-tpl-123']);
    });

    it('devrait sélectionner un modèle type (Starter), pré-remplir les données et créer les variables associées', async () => {
      const navigateSpy = vi.spyOn(router, 'navigate');
      const compiled = fixture.nativeElement as HTMLElement;

      // On cible une carte starter prédéfinie
      const starterCards = compiled.querySelectorAll<HTMLElement>('.starter-grid .starter-card:not(.blank-card)');
      expect(starterCards.length).toBeGreaterThan(0);

      const firstStarterCard = starterCards[0];
      firstStarterCard.click();
      fixture.detectChanges();
      await fixture.whenStable();

      // La carte sélectionnée doit avoir la classe CSS 'selected'
      expect(firstStarterCard.classList.contains('selected')).toBe(true);
      expect(component.selectedStarter).not.toBeNull();

      // Le bandeau d'aperçu des variables doit s'afficher dans le DOM
      const previewBanner = compiled.querySelector('.starter-preview-banner');
      expect(previewBanner).not.toBeNull();

      // Le formulaire a été mis à jour automatiquement avec le nom du starter
      expect(component.form.get('nom')?.value).toBe(component.selectedStarter!.nom);
      expect(component.form.valid).toBe(true);

      const submitBtn = compiled.querySelector<HTMLButtonElement>('button[type="submit"]')!;
      expect(submitBtn.disabled).toBe(false);

      // Clic de soumission
      submitBtn.click();
      fixture.detectChanges();

      // Requête de création du template avec contenuDesign
      const reqCreate = httpTesting.expectOne('/api/templates');
      expect(reqCreate.request.method).toBe('POST');
      expect(reqCreate.request.body.nom).toBe(component.selectedStarter!.nom);
      expect(reqCreate.request.body.contenuDesign).toBeDefined();

      const createdTemplateId = 'starter-created-999';
      reqCreate.flush({
        id: createdTemplateId,
        nom: component.selectedStarter!.nom,
        statut: 'BROUILLON',
        version: 1,
      });

      // Si le starter contient des variables, TemplateCreate appelle addVariable pour chacune en parallèle
      const starterVars = component.selectedStarter!.variables || [];
      if (starterVars.length > 0) {
        const varRequests = httpTesting.match(`/api/templates/${createdTemplateId}/variables`);
        expect(varRequests.length).toBe(starterVars.length);
        varRequests.forEach((req, idx) => {
          expect(req.request.method).toBe('POST');
          req.flush({ id: 'v-' + idx, nomVariable: req.request.body.nomVariable });
        });
      }

      fixture.detectChanges();
      await fixture.whenStable();

      expect(navigateSpy).toHaveBeenCalledWith(['/templates', createdTemplateId]);
    });
  });

  describe('3. Gestion des formulaires et cas aux limites', () => {
    it('devrait afficher le message d\'erreur de validation HTML quand le champ nom obligatoire est touché et vide', async () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const nomInput = compiled.querySelector<HTMLInputElement>('#nom')!;
      const submitBtn = compiled.querySelector<HTMLButtonElement>('button[type="submit"]')!;

      // Au départ, pas d'erreur affichée
      let fieldError = compiled.querySelector('.form-group .error');
      expect(fieldError).toBeNull();

      // L'utilisateur touche le champ puis en sort sans rien saisir
      nomInput.dispatchEvent(new Event('focus'));
      nomInput.value = '';
      nomInput.dispatchEvent(new Event('input'));
      nomInput.dispatchEvent(new Event('blur'));

      component.form.get('nom')?.markAsTouched();
      fixture.detectChanges();
      await fixture.whenStable();

      // Le message d'erreur doit maintenant être affiché dans le DOM
      fieldError = compiled.querySelector('.form-group .error');
      expect(fieldError).not.toBeNull();
      expect(fieldError?.textContent?.trim()).toBe('Le nom du modèle est obligatoire.');

      // Le bouton de soumission doit rester désactivé
      expect(submitBtn.disabled).toBe(true);

      // Aucune requête HTTP ne doit être envoyée
      httpTesting.expectNone('/api/templates');
    });

    it('devrait filtrer les cartes de modèles lorsque l\'utilisateur clique sur un onglet de catégorie', async () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const tabs = compiled.querySelectorAll<HTMLButtonElement>('.category-tabs .tab-btn');

      // Trouver l'onglet Ventes
      const ventesTab = Array.from(tabs).find((b) => b.textContent?.includes('Ventes'));
      expect(ventesTab).toBeDefined();

      ventesTab!.click();
      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.selectedCategory).toBe('VENTES');
      expect(ventesTab!.classList.contains('active')).toBe(true);

      // Toutes les cartes affichées (hors feuille vierge) doivent appartenir à la catégorie VENTES
      const visibleStarters = component.filteredStarters;
      expect(visibleStarters.every((s) => s.categorie === 'VENTES')).toBe(true);
    });
  });

  describe('4. Gestion des erreurs asynchrones', () => {
    it('devrait afficher un message d\'erreur dans le DOM si l\'API backend renvoie une erreur 500 lors de la création', async () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const nomInput = compiled.querySelector<HTMLInputElement>('#nom')!;
      const submitBtn = compiled.querySelector<HTMLButtonElement>('button[type="submit"]')!;

      nomInput.value = 'Modèle qui échouera';
      nomInput.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      await fixture.whenStable();

      submitBtn.click();
      fixture.detectChanges();

      const req = httpTesting.expectOne('/api/templates');
      expect(req.request.method).toBe('POST');

      // Simulation d'une erreur 500 du serveur
      req.flush(
        { message: 'Impossible d\'enregistrer le modèle en base de données' },
        { status: 500, statusText: 'Internal Server Error' }
      );

      fixture.detectChanges();
      await fixture.whenStable();

      // Vérification de la réaction de l'interface
      expect(component.submitting).toBe(false);
      const errorMsg = compiled.querySelector('.error.message');
      expect(errorMsg).not.toBeNull();
      expect(errorMsg?.textContent?.trim()).toBe('Erreur lors de la création du modèle.');

      // Le bouton doit être réactivé
      expect(submitBtn.disabled).toBe(false);
    });
  });
});
