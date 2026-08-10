import { Component, ChangeDetectorRef } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { CodeEntrepriseModal } from './code-entreprise-modal/code-entreprise-modal';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, CodeEntrepriseModal],
  template: `
    <app-code-entreprise-modal
      *ngIf="showModal"
      (submitted)="onCodeSubmitted($event)">
    </app-code-entreprise-modal>

    <nav class="navbar">
      <a routerLink="/bibliotheque" routerLinkActive="active">Bibliothèque</a>
      <a routerLink="/templates/new" routerLinkActive="active">Créer</a>
      <a routerLink="/documents" routerLinkActive="active">Mes documents</a>
    </nav>
    <main>
      <router-outlet></router-outlet>
    </main>
  `,
  styleUrls: ['./app.scss'],
})
export class App {
  showModal = false;

  constructor(private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    const code = sessionStorage.getItem('entrepriseCode');
    if (!code) {
      this.showModal = true;
    }
  }

  onCodeSubmitted(code: string): void {
    localStorage.setItem('entrepriseCode', code);
    this.showModal = false;
    // Pas besoin de recharger la page, l'interceptor utilisera le nouveau code.
    this.cdr.detectChanges();
  }
}