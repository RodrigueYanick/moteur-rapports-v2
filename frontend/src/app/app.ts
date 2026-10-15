import { Component, ChangeDetectorRef, OnInit, OnDestroy } from '@angular/core';
import { Router, NavigationEnd, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Subscription, filter } from 'rxjs';
import { CodeEntrepriseModal } from '@shared/components/code-entreprise-modal/code-entreprise-modal';
import { AuthService } from '@services/auth.service';
import { ToastContainerComponent } from '@shared/components/toast-container/toast-container.component';
import { ThemeService } from '@shared/services/theme.service';
import { CommandPaletteComponent } from '@shared/components/command-palette/command-palette.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    CommonModule,
    CodeEntrepriseModal,
    ToastContainerComponent,
    CommandPaletteComponent,
  ],
  template: `
    <a href="#main-content" class="skip-link">Passer au contenu principal</a>
    <app-code-entreprise-modal
      *ngIf="showModal"
      (submitted)="onCodeSubmitted($event)">
    </app-code-entreprise-modal>

    <app-toast-container></app-toast-container>
    <app-command-palette [(isOpen)]="commandPaletteOpen"></app-command-palette>

    <nav class="navbar">
      <div class="nav-left">
        <!-- Bouton Menu Hamburger Mobile -->
        <button
          type="button"
          class="nav-hamburger-btn"
          (click)="toggleMobileMenu()"
          [attr.aria-expanded]="mobileMenuOpen"
          aria-label="Menu de navigation"
          *ngIf="auth.isAuthenticated()">
          <span class="hamburger-icon">{{ mobileMenuOpen ? '✕' : '☰' }}</span>
        </button>

        <a routerLink="/" class="nav-brand" (click)="closeMobileMenu()">
          <span class="brand-icon">📑</span>
          <span class="brand-text">Moteur de Rapports</span>
        </a>
      </div>

      <!-- Liens Desktop -->
      <div class="nav-links desktop-only" *ngIf="auth.isAuthenticated()">
        <a routerLink="/bibliotheque" routerLinkActive="active">Modèles</a>
        <a routerLink="/documents" routerLinkActive="active">Mes documents</a>
        <a routerLink="/feuille-travail" routerLinkActive="active">Personnaliser</a>

        <!-- Menu Entreprise uniquement -->
        <ng-container *ngIf="auth.isEnterprise()">
          <a routerLink="/batches" routerLinkActive="active">Lots & Webhooks</a>
          <a routerLink="/data-sources" routerLinkActive="active">Connecteurs</a>
          <a routerLink="/schedules" routerLinkActive="active">Planification</a>
          <a routerLink="/certificates" routerLinkActive="active">Certificats</a>
          <a routerLink="/audit" routerLinkActive="active" *ngIf="auth.currentRole() === 'SUPER_ADMIN' || auth.currentRole() === 'ADMIN_ENTREPRISE'">Audit</a>
        </ng-container>

        <!-- CTA Espace Entreprise pour compte personnel -->
        <a routerLink="/auth/register" [queryParams]="{ accountType: 'ENTERPRISE' }" class="upgrade-enterprise-link" *ngIf="auth.isPersonal()" title="Découvrir la suite Entreprise (Lots, Signatures X.509, SQL)">
          🏢 Espace Entreprise
        </a>
      </div>

      <div class="nav-right">
        <!-- Bouton d'ouverture de la palette de commande -->
        <button
          type="button"
          class="nav-search-btn"
          (click)="commandPaletteOpen = true"
          title="Rechercher ou actionner (Ctrl+K)">
          <span class="search-icon">🔍</span>
          <span class="search-text desktop-only">Rechercher...</span>
          <kbd class="search-kbd desktop-only">Ctrl K</kbd>
        </button>

        <!-- Bouton bascule de thème Dark / Light -->
        <button
          type="button"
          class="theme-toggle-btn"
          (click)="themeService.toggleTheme()"
          [title]="themeService.isDark ? 'Passer en mode clair' : 'Passer en mode sombre'">
          <span>{{ themeService.isDark ? '☀️' : '🌙' }}</span>
        </button>

        <ng-container *ngIf="auth.isAuthenticated(); else loginLink">
          <div class="user-info-pill desktop-only">
            <span class="user-name">👤 {{ auth.currentUser()?.nomComplet || auth.currentUser()?.email }}</span>
            <span class="role-badge" [ngClass]="auth.currentRole() || ''">{{ formatRole(auth.currentRole()) }}</span>
          </div>

          <button type="button" class="company-badge-btn desktop-only" (click)="openChangeCodeModal()" title="Entreprise active" *ngIf="auth.isEnterprise()">
            <span class="badge-icon">🏢</span>
            <span class="badge-code">{{ auth.currentEntrepriseCode() }}</span>
            <span class="badge-edit" *ngIf="auth.currentRole() === 'SUPER_ADMIN'">✎</span>
          </button>

          <div class="personal-badge-pill desktop-only" *ngIf="auth.isPersonal()">
            <span>✨ Compte Gratuit</span>
          </div>

          <button type="button" class="logout-btn desktop-only" (click)="auth.logout()" title="Se déconnecter">
            <span>Déconnexion</span>
          </button>
        </ng-container>

        <ng-template #loginLink>
          <div class="nav-visitor-actions desktop-only">
            <a routerLink="/public" class="visitor-nav-link">Explorer</a>
            <a routerLink="/auth/register" [queryParams]="{ accountType: 'ENTERPRISE' }" class="visitor-enterprise-link">Espace Entreprise</a>
          </div>
          <a routerLink="/auth/login" class="login-nav-btn">Connexion</a>
        </ng-template>
      </div>
    </nav>

    <!-- Mobile Drawer Backdrop & Menu -->
    <div
      class="mobile-drawer-backdrop"
      *ngIf="mobileMenuOpen && auth.isAuthenticated()"
      (click)="closeMobileMenu()">
    </div>

    <aside
      class="mobile-nav-drawer"
      [class.open]="mobileMenuOpen && auth.isAuthenticated()"
      *ngIf="auth.isAuthenticated()">
      <div class="mobile-drawer-header">
        <div class="user-info-pill-mobile">
          <span class="user-name">👤 {{ auth.currentUser()?.nomComplet || auth.currentUser()?.email }}</span>
          <span class="role-badge" [ngClass]="auth.currentRole() || ''">{{ formatRole(auth.currentRole()) }}</span>
        </div>
        <button type="button" class="close-drawer-btn" (click)="closeMobileMenu()" aria-label="Fermer le menu">✕</button>
      </div>

      <div class="mobile-drawer-company" *ngIf="auth.isEnterprise()">
        <button type="button" class="company-badge-btn-mobile" (click)="openChangeCodeModal(); closeMobileMenu()" title="Entreprise active">
          <span class="badge-icon">🏢</span>
          <span class="badge-text">Entreprise active :</span>
          <span class="badge-code">{{ auth.currentEntrepriseCode() }}</span>
          <span class="badge-edit" *ngIf="auth.currentRole() === 'SUPER_ADMIN'">✎</span>
        </button>
      </div>

      <nav class="mobile-drawer-links">
        <a routerLink="/bibliotheque" routerLinkActive="active" (click)="closeMobileMenu()">
          <span class="nav-icon">📚</span>
          <span>Modèles</span>
        </a>
        <a routerLink="/documents" routerLinkActive="active" (click)="closeMobileMenu()">
          <span class="nav-icon">📄</span>
          <span>Mes documents</span>
        </a>
        <a routerLink="/feuille-travail" routerLinkActive="active" (click)="closeMobileMenu()">
          <span class="nav-icon">📐</span>
          <span>Personnaliser la feuille</span>
        </a>

        <!-- Liens Entreprise uniquement -->
        <ng-container *ngIf="auth.isEnterprise()">
          <a routerLink="/batches" routerLinkActive="active" (click)="closeMobileMenu()">
            <span class="nav-icon">📦</span>
            <span>Lots & Webhooks</span>
          </a>
          <a routerLink="/data-sources" routerLinkActive="active" (click)="closeMobileMenu()">
            <span class="nav-icon">🔌</span>
            <span>Connecteurs</span>
          </a>
          <a routerLink="/schedules" routerLinkActive="active" (click)="closeMobileMenu()">
            <span class="nav-icon">⏰</span>
            <span>Planification</span>
          </a>
          <a routerLink="/certificates" routerLinkActive="active" (click)="closeMobileMenu()">
            <span class="nav-icon">🔐</span>
            <span>Certificats</span>
          </a>
          <a routerLink="/audit" routerLinkActive="active" *ngIf="auth.currentRole() === 'SUPER_ADMIN' || auth.currentRole() === 'ADMIN_ENTREPRISE'" (click)="closeMobileMenu()">
            <span class="nav-icon">🛡️</span>
            <span>Audit & Sécurité</span>
          </a>
        </ng-container>

        <!-- Passer en mode Entreprise depuis un compte personnel -->
        <a routerLink="/auth/register" [queryParams]="{ accountType: 'ENTERPRISE' }" class="upgrade-drawer-link" *ngIf="auth.isPersonal()" (click)="closeMobileMenu()">
          <span class="nav-icon">🏢</span>
          <span>Créer un Espace Entreprise</span>
        </a>
      </nav>

      <div class="mobile-drawer-footer">
        <button type="button" class="logout-btn-mobile" (click)="auth.logout(); closeMobileMenu()">
          <span>🚪 Déconnexion</span>
        </button>
      </div>
    </aside>

    <main id="main-content" tabindex="-1">
      <router-outlet></router-outlet>
    </main>
  `,
  styleUrls: ['./app.scss'],
})
export class App implements OnInit, OnDestroy {
  showModal = false;
  commandPaletteOpen = false;
  mobileMenuOpen = false;
  private routerSub?: Subscription;

  constructor(
    public auth: AuthService,
    public themeService: ThemeService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.routerSub = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        this.closeMobileMenu();
      });
  }

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen = !this.mobileMenuOpen;
    this.cdr.detectChanges();
  }

  closeMobileMenu(): void {
    if (this.mobileMenuOpen) {
      this.mobileMenuOpen = false;
      this.cdr.detectChanges();
    }
  }

  openChangeCodeModal(): void {
    if (this.auth.currentRole() === 'SUPER_ADMIN') {
      this.showModal = true;
      this.cdr.detectChanges();
    }
  }

  onCodeSubmitted(code: string): void {
    localStorage.setItem('entrepriseCode', code);
    this.showModal = false;
    this.cdr.detectChanges();
  }

  formatRole(role: string | null): string {
    switch (role) {
      case 'SUPER_ADMIN': return 'Super Admin';
      case 'ADMIN_ENTREPRISE': return 'Admin';
      case 'DESIGNER': return 'Designer';
      case 'OPERATOR': return 'Opérateur';
      default: return role || '';
    }
  }
}