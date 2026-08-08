import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TemplateApiService } from '../../services/template-api';
import { Template } from '../../models/template.model';

@Component({
  selector: 'app-template-library',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './template-library.html',
  styleUrls: ['./template-library.scss']
})
export class TemplateLibrary implements OnInit {
  templates: Template[] = [];
  filteredTemplates: Template[] = [];
  loading = false;
  error = '';

  // Filtres
  filterCategorie = '';
  filterStatut = '';

  categories: string[] = ['VENTES', 'ACHATS', 'FINANCE', 'RH', 'LOGISTIQUE', 'STOCK', 'PRODUCTION', 'ADMINISTRATION', 'AUTRES'];
  statuts: string[] = ['BROUILLON', 'PUBLIE', 'ARCHIVE'];

  constructor(private api: TemplateApiService) {}

  ngOnInit(): void {
    this.loadTemplates();
  }

  loadTemplates(): void {
    this.loading = true;
    this.api.getTemplates().subscribe({
      next: (templates) => {
        this.templates = templates;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des modèles.';
        this.loading = false;
        console.error(err);
      }
    });
  }

  applyFilters(): void {
    this.filteredTemplates = this.templates.filter(t => {
      const matchCategorie = !this.filterCategorie || t.categorie === this.filterCategorie;
      const matchStatut = !this.filterStatut || t.statut === this.filterStatut;
      return matchCategorie && matchStatut;
    });
  }

  duplicateTemplate(id: string): void {
    this.api.duplicateTemplate(id).subscribe({
      next: () => this.loadTemplates(),
      error: (err) => console.error('Erreur duplication', err)
    });
  }

  deleteTemplate(id: string): void {
    if (confirm('Supprimer ce modèle ?')) {
      this.api.deleteTemplate(id).subscribe({
        next: () => this.loadTemplates(),
        error: (err) => console.error('Erreur suppression', err)
      });
    }
  }

  getStatusClass(statut: string): string {
    return statut.toLowerCase();
  }
}