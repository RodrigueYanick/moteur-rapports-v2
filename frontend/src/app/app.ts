import { Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar">
      <a routerLink="/templates" routerLinkActive="Active" [routerLinkActiveOptions]="{exact: false}">Modeles</a>
      <a routerLink="/templates/new" routerLinkActive="Active">Creer</a>
    </nav>
    <main>
      <router-outlet></router-outlet>
    </main>
  `,
  styleUrls: ['./app.scss']
})
export class App {
  protected readonly title = signal('report-designer');
}
