import { Routes } from '@angular/router';
import { TemplateList } from './templates/template-list/template-list';
import { TemplateCreate } from './templates/template-create/template-create';
import { TemplateDetail } from './templates/template-detail/template-detail';

export const routes: Routes = [
    {path: '', redirectTo: '/templates', pathMatch: 'full'},
    {path: 'templates', component:TemplateList},
    {path: 'templates/new', component:TemplateCreate},
    {path: 'templates/:id', component:TemplateDetail}
];
