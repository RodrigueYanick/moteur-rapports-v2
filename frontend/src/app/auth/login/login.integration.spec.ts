import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router, ActivatedRoute } from '@angular/router';
import { LoginComponent } from './login.component';
import { AuthService } from '../../services/auth.service';

describe('LoginComponent - Tests d\'Intégration Complets', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let httpTesting: HttpTestingController;
  let router: Router;
  let authService: AuthService;

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParams: { returnUrl: '/bibliotheque' },
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    authService = TestBed.inject(AuthService);

    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  describe('1. Rendu initial et cycle de vie', () => {
    it('devrait compiler le template HTML et afficher le formulaire de connexion avec ses éléments de base', () => {
      const compiled = fixture.nativeElement as HTMLElement;

      const title = compiled.querySelector('h1');
      expect(title?.textContent?.trim()).toBe('Connexion');

      const emailInput = compiled.querySelector<HTMLInputElement>('#email');
      const passwordInput = compiled.querySelector<HTMLInputElement>('#password');
      const submitBtn = compiled.querySelector<HTMLButtonElement>('.submit-btn');
      const demoBtns = compiled.querySelectorAll('.demo-btn');

      expect(emailInput).not.toBeNull();
      expect(emailInput?.value).toBe('');
      expect(passwordInput).not.toBeNull();
      expect(passwordInput?.value).toBe('');
      expect(submitBtn).not.toBeNull();
      expect(submitBtn?.disabled).toBe(false);
      expect(demoBtns.length).toBe(2);

      // Aucun bandeau d'erreur ne doit être présent initialement
      const errorBanner = compiled.querySelector('.error-banner');
      expect(errorBanner).toBeNull();
    });
  });

  describe('2. Interactions du DOM (Happy Path)', () => {
    it('devrait permettre la saisie utilisateur, la soumission et l\'authentification avec redirection', async () => {
      const navigateSpy = vi.spyOn(router, 'navigateByUrl');
      const compiled = fixture.nativeElement as HTMLElement;

      const emailInput = compiled.querySelector<HTMLInputElement>('#email')!;
      const passwordInput = compiled.querySelector<HTMLInputElement>('#password')!;
      const submitBtn = compiled.querySelector<HTMLButtonElement>('.submit-btn')!;

      // Simulation de la saisie utilisateur
      emailInput.value = 'user@entreprise.com';
      emailInput.dispatchEvent(new Event('input'));

      passwordInput.value = 'Secret123!';
      passwordInput.dispatchEvent(new Event('input'));

      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.email).toBe('user@entreprise.com');
      expect(component.motDePasse).toBe('Secret123!');

      // Clic sur le bouton de soumission
      submitBtn.click();
      fixture.detectChanges();

      // Vérification de l'état loading dans le DOM
      expect(component.loading).toBe(true);
      expect(submitBtn.disabled).toBe(true);
      expect(submitBtn.textContent).toContain('Connexion en cours...');

      // Interception de l'appel HTTP réel
      const req = httpTesting.expectOne('/api/auth/login');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        email: 'user@entreprise.com',
        motDePasse: 'Secret123!',
      });

      // Réponse HTTP réussie
      req.flush({
        token: 'jwt-token-xyz',
        user: {
          id: 'u-1',
          email: 'user@entreprise.com',
          nomComplet: 'Jean Dupont',
          role: 'ADMIN_ENTREPRISE',
          codeEntreprise: 'ENT-999',
        },
      });

      fixture.detectChanges();
      await fixture.whenStable();

      // Vérification des répercussions métier et UI
      expect(component.loading).toBe(false);
      expect(localStorage.getItem('auth_token')).toBe('jwt-token-xyz');
      expect(localStorage.getItem('entrepriseCode')).toBe('ENT-999');
      expect(authService.isAuthenticated()).toBe(true);
      expect(authService.currentRole()).toBe('ADMIN_ENTREPRISE');
      expect(navigateSpy).toHaveBeenCalledWith('/bibliotheque');
    });

    it('devrait connecter un profil Admin via le bouton d\'accès rapide Démo', async () => {
      const navigateSpy = vi.spyOn(router, 'navigateByUrl');
      const compiled = fixture.nativeElement as HTMLElement;

      const adminDemoBtn = compiled.querySelector<HTMLButtonElement>('.demo-btn.admin')!;
      expect(adminDemoBtn).not.toBeNull();

      // Clic sur le bouton démo admin
      adminDemoBtn.click();
      fixture.detectChanges();

      const req = httpTesting.expectOne('/api/auth/login');
      expect(req.request.body).toEqual({
        email: 'admin@rapports.com',
        motDePasse: 'admin123',
      });

      req.flush({
        token: 'token-admin',
        user: {
          id: 'admin-1',
          email: 'admin@rapports.com',
          role: 'SUPER_ADMIN',
          codeEntreprise: 'ENT-ROOT',
        },
      });

      fixture.detectChanges();
      await fixture.whenStable();

      expect(authService.currentRole()).toBe('SUPER_ADMIN');
      expect(navigateSpy).toHaveBeenCalledWith('/bibliotheque');
    });
  });

  describe('3. Gestion des formulaires et cas aux limites', () => {
    it('devrait afficher un message d\'erreur dans le DOM sans appel HTTP si les champs sont vides', async () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const submitBtn = compiled.querySelector<HTMLButtonElement>('.submit-btn')!;

      // Clic direct sans renseigner d'identifiants
      submitBtn.click();
      fixture.detectChanges();
      await fixture.whenStable();

      // Aucune requête HTTP ne doit être déclenchée
      httpTesting.expectNone('/api/auth/login');

      // Le bandeau d'erreur doit être injecté dynamiquement dans le template HTML
      const errorBanner = compiled.querySelector('.error-banner');
      expect(errorBanner).not.toBeNull();
      expect(errorBanner?.textContent?.trim()).toBe('Veuillez renseigner votre email et mot de passe.');
      expect(component.loading).toBe(false);
    });

    it('devrait afficher l\'erreur si seulement l\'email est renseigné', async () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const emailInput = compiled.querySelector<HTMLInputElement>('#email')!;
      const submitBtn = compiled.querySelector<HTMLButtonElement>('.submit-btn')!;

      emailInput.value = 'incomplet@test.com';
      emailInput.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      await fixture.whenStable();

      submitBtn.click();
      fixture.detectChanges();
      await fixture.whenStable();

      httpTesting.expectNone('/api/auth/login');
      const errorBanner = compiled.querySelector('.error-banner');
      expect(errorBanner?.textContent?.trim()).toBe('Veuillez renseigner votre email et mot de passe.');
    });
  });

  describe('4. Gestion des erreurs asynchrones', () => {
    it('devrait afficher le message d\'erreur renvoyé par le backend lors d\'une erreur 401', async () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const emailInput = compiled.querySelector<HTMLInputElement>('#email')!;
      const passwordInput = compiled.querySelector<HTMLInputElement>('#password')!;
      const submitBtn = compiled.querySelector<HTMLButtonElement>('.submit-btn')!;

      emailInput.value = 'wrong@test.com';
      emailInput.dispatchEvent(new Event('input'));
      passwordInput.value = 'badpassword';
      passwordInput.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      await fixture.whenStable();

      submitBtn.click();
      fixture.detectChanges();

      const req = httpTesting.expectOne('/api/auth/login');
      req.flush(
        { message: 'Mot de passe incorrect.' },
        { status: 401, statusText: 'Unauthorized' }
      );

      fixture.detectChanges();
      await fixture.whenStable();

      expect(component.loading).toBe(false);
      const errorBanner = compiled.querySelector('.error-banner');
      expect(errorBanner).not.toBeNull();
      expect(errorBanner?.textContent?.trim()).toBe('Mot de passe incorrect.');
    });

    it('devrait afficher le premier message d\'erreur si le backend renvoie un tableau d\'erreurs', async () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const emailInput = compiled.querySelector<HTMLInputElement>('#email')!;
      const passwordInput = compiled.querySelector<HTMLInputElement>('#password')!;
      const submitBtn = compiled.querySelector<HTMLButtonElement>('.submit-btn')!;

      emailInput.value = 'disabled@test.com';
      emailInput.dispatchEvent(new Event('input'));
      passwordInput.value = 'password123';
      passwordInput.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      await fixture.whenStable();

      submitBtn.click();
      fixture.detectChanges();

      const req = httpTesting.expectOne('/api/auth/login');
      req.flush(
        { errors: ['Ce compte a été suspendu par l\'administrateur.'] },
        { status: 403, statusText: 'Forbidden' }
      );

      fixture.detectChanges();
      await fixture.whenStable();

      const errorBanner = compiled.querySelector('.error-banner');
      expect(errorBanner?.textContent?.trim()).toBe('Ce compte a été suspendu par l\'administrateur.');
      expect(component.loading).toBe(false);
    });

    it('devrait afficher un message d\'alerte générique en cas de panne serveur HTTP 500', async () => {
      const compiled = fixture.nativeElement as HTMLElement;
      const emailInput = compiled.querySelector<HTMLInputElement>('#email')!;
      const passwordInput = compiled.querySelector<HTMLInputElement>('#password')!;
      const submitBtn = compiled.querySelector<HTMLButtonElement>('.submit-btn')!;

      emailInput.value = 'crash@test.com';
      emailInput.dispatchEvent(new Event('input'));
      passwordInput.value = 'password123';
      passwordInput.dispatchEvent(new Event('input'));
      fixture.detectChanges();
      await fixture.whenStable();

      submitBtn.click();
      fixture.detectChanges();

      const req = httpTesting.expectOne('/api/auth/login');
      req.flush(null, { status: 500, statusText: 'Internal Server Error' });

      fixture.detectChanges();
      await fixture.whenStable();

      const errorBanner = compiled.querySelector('.error-banner');
      expect(errorBanner).not.toBeNull();
      expect(errorBanner?.textContent?.trim()).toBe('Identifiants invalides ou service indisponible.');
      expect(component.loading).toBe(false);
    });
  });
});

