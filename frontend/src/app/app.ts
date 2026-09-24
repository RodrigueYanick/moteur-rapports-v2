import { Component, ChangeDetectorRef, OnInit } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
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
      <div class="nav-brand">
        <span class="brand-icon">📑</span>
        <span class="brand-text">Moteur de Rapports</span>
      </div>

      <div class="nav-links" *ngIf="auth.isAuthenticated()">
        <a routerLink="/bibliotheque" routerLinkActive="active">Bibliothèque</a>
        <a routerLink="/documents" routerLinkActive="active">Mes documents</a>
        <a routerLink="/batches" routerLinkActive="active">Lots & Webhooks</a>
        <a routerLink="/feuille-travail" routerLinkActive="active">Personnaliser la feuille</a>
      </div>

      <div class="nav-right">
        <!-- Bouton d'ouverture de la palette de commande -->
        <button
          type="button"
          class="nav-search-btn"
          (click)="commandPaletteOpen = true"
          title="Rechercher ou actionner (Ctrl+K)">
          <span class="search-icon">🔍</span>
          <span class="search-text">Rechercher...</span>
          <kbd class="search-kbd">Ctrl K</kbd>
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
          <div class="user-info-pill">
            <span class="user-name">👤 {{ auth.currentUser()?.nomComplet || auth.currentUser()?.email }}</span>
            <span class="role-badge" [ngClass]="auth.currentRole() || ''">{{ formatRole(auth.currentRole()) }}</span>
          </div>

          <button type="button" class="company-badge-btn" (click)="openChangeCodeModal()" title="Entreprise active">
            <span class="badge-icon">🏢</span>
            <span class="badge-code">{{ auth.currentEntrepriseCode() }}</span>
            <span class="badge-edit" *ngIf="auth.currentRole() === 'SUPER_ADMIN'">✎</span>
          </button>

          <button type="button" class="logout-btn" (click)="auth.logout()" title="Se déconnecter">
            <span>Déconnexion</span>
          </button>
        </ng-container>

        <ng-template #loginLink>
          <a routerLink="/auth/login" class="login-nav-btn">Connexion</a>
        </ng-template>
      </div>
    </nav>

    <main id="main-content" tabindex="-1">
      <router-outlet></router-outlet>
    </main>
  `,
  styleUrls: ['./app.scss'],
})
export class App implements OnInit {
  showModal = false;
  commandPaletteOpen = false;

  constructor(
    public auth: AuthService,
    public themeService: ThemeService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {}

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