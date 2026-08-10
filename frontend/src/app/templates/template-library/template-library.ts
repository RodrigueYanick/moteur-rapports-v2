import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TemplateApiService } from '../../services/template-api';
import { Template } from '../../models/template.model';
import {
  LucideAngularModule, Search, Plus, LayoutGrid, List as ListIcon,
  Star, Eye, Edit3, Copy, Trash2, FileText, Loader2
} from 'lucide-angular';

@Component({
  selector: 'app-template-library',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, LucideAngularModule],
  templateUrl: './template-library.html',
  styleUrls: ['./template-library.scss'],
})
export class TemplateLibrary implements OnInit {
  templates: Template[] = [];
  filteredTemplates: Template[] = [];
  loading = false;
  error = '';

  searchTerm = '';
  filterCategorie = '';
  filterStatut = '';
  viewMode: 'grid' | 'list' = 'grid';

  categories: string[] = [
    'VENTES', 'ACHATS', 'FINANCE', 'RH', 'LOGISTIQUE',
    'STOCK', 'PRODUCTION', 'ADMINISTRATION', 'AUTRES',
  ];
  statuts: string[] = ['BROUILLON', 'PUBLIE', 'ARCHIVE'];

  readonly icons = {
    search: Search, plus: Plus, grid: LayoutGrid, list: ListIcon,
    star: Star, eye: Eye, edit: Edit3, copy: Copy, trash: Trash2,
    file: FileText, loader: Loader2
  };

  // Palette cyclique attribuée par catégorie, pour un rendu coloré cohérent
  private categoryColors: Record<string, string> = {
    VENTES: 'linear-gradient(135deg, #4338ca, #6d5efc)',
    ACHATS: 'linear-gradient(135deg, #ea580c, #f97316)',
    FINANCE: 'linear-gradient(135deg, #047857, #10b981)',
    RH: 'linear-gradient(135deg, #7c3aed, #a78bfa)',
    LOGISTIQUE: 'linear-gradient(135deg, #0369a1, #38bdf8)',
    STOCK: 'linear-gradient(135deg, #0f766e, #2dd4bf)',
    PRODUCTION: 'linear-gradient(135deg, #b91c1c, #ef4444)',
    ADMINISTRATION: 'linear-gradient(135deg, #1e293b, #475569)',
    AUTRES: 'linear-gradient(135deg, #52525b, #a1a1aa)',
  };

  constructor(
    private api: TemplateApiService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadTemplates();
  }

  loadTemplates(): void {
    this.loading = true;
    this.error = '';
    this.api.getTemplates().subscribe({
      next: (templates) => {
        this.templates = templates;
        this.applyFilters();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des modèles.';
        this.loading = false;
        this.cdr.detectChanges();
        console.error(err);
      },
    });
  }

  applyFilters(): void {
    const term = this.searchTerm.trim().toLowerCase();
    this.filteredTemplates = this.templates.filter((t) => {
      const matchCategorie = !this.filterCategorie || t.categorie === this.filterCategorie;
      const matchStatut = !this.filterStatut || t.statut === this.filterStatut;
      const matchSearch = !term || t.nom.toLowerCase().includes(term);
      return matchCategorie && matchStatut && matchSearch;
    });
  }

  setViewMode(mode: 'grid' | 'list'): void {
    this.viewMode = mode;
  }

  cardBackground(template: Template): string {
    return this.categoryColors[template.categorie || 'AUTRES'] || this.categoryColors['AUTRES'];
  }

  initials(nom: string): string {
    return nom
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map(w => w[0]?.toUpperCase())
      .join('');
  }

  duplicateTemplate(id: string, event?: Event): void {
    event?.stopPropagation();
    this.api.duplicateTemplate(id).subscribe({
      next: () => this.loadTemplates(),
      error: (err) => {
        console.error('Erreur duplication', err);
        this.cdr.detectChanges();
      },
    });
  }

  deleteTemplate(id: string, event?: Event): void {
    event?.stopPropagation();
    if (confirm('Supprimer ce modèle ?')) {
      this.api.deleteTemplate(id).subscribe({
        next: () => this.loadTemplates(),
        error: (err) => {
          console.error('Erreur suppression', err);
          this.cdr.detectChanges();
        },
      });
    }
  }

  getStatusClass(statut: string): string {
    return statut.toLowerCase();
  }
}