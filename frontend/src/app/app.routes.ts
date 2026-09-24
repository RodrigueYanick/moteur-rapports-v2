import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/bibliotheque', pathMatch: 'full' },
  {
    path: 'auth/login',
    loadComponent: () => import('./auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'auth/register',
    loadComponent: () => import('./auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'bibliotheque',
    loadComponent: () => import('./templates/template-library/template-library').then(m => m.TemplateLibrary),
    canActivate: [authGuard]
  },
  {
    path: 'feuille-travail',
    loadComponent: () => import('./workspace-config/workspace-config').then(m => m.WorkspaceConfigComponent),
    canActivate: [authGuard]
  },
  {
    path: 'templates/new',
    loadComponent: () => import('./templates/template-create/template-create').then(m => m.TemplateCreate),
    canActivate: [authGuard]
  },
  {
    path: 'templates/:id',
    loadComponent: () => import('./templates/template-detail/template-detail').then(m => m.TemplateDetail),
    canActivate: [authGuard]
  },
  {
    path: 'documents',
    loadComponent: () => import('./documents/document-list/document-list').then(m => m.DocumentList),
    canActivate: [authGuard]
  },
  {
    path: 'batches',
    loadComponent: () => import('./batches/batch-list/batch-list').then(m => m.BatchListComponent),
    canActivate: [authGuard]
  },
  { path: '**', redirectTo: '/bibliotheque' }
];
