import { Routes } from '@angular/router';
import { TemplateList } from './templates/template-list/template-list';
import { TemplateDetail } from './templates/template-detail/template-detail';
import { TemplateCreate } from './templates/template-create/template-create';
import { TemplateLibrary } from './templates/template-library/template-library';

export const routes: Routes = [
  { path: '', redirectTo: '/bibliotheque', pathMatch: 'full' },
  { path: 'bibliotheque', component: TemplateLibrary },
  { path: 'templates/new', component: TemplateCreate },
  { path: 'templates/:id', component: TemplateDetail }
];
