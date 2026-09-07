import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';  // <-- ajout Router
import { FormsModule } from '@angular/forms';
import { TemplateApiService } from '../../services/template-api';
import { Template } from '../../models/template.model';
import {
  LucideAngularModule, Search, Plus, LayoutGrid, List as ListIcon,
  Star, Eye, Edit3, Copy, Trash2, FileText, Loader2, Archive, RefreshCw, RotateCcw
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
  visibilityFilter: 'ALL' | 'PUBLIC' | 'PRIVATE' = 'ALL';

  categories: string[] = [
    'VENTES', 'ACHATS', 'FINANCE', 'RH', 'LOGISTIQUE',
    'STOCK', 'PRODUCTION', 'ADMINISTRATION', 'AUTRES',
  ];
  statuts: string[] = ['BROUILLON', 'PUBLIE', 'ARCHIVE'];

  readonly icons = {
    search: Search, plus: Plus, grid: LayoutGrid, list: ListIcon,
    star: Star, eye: Eye, edit: Edit3, copy: Copy, trash: Trash2,
    file: FileText, loader: Loader2, archive: Archive, newVersion: RefreshCw, restore: RotateCcw
  };

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
    private cdr: ChangeDetectorRef,
    private router: Router    // <-- injection ajoutée
  ) {}

  ngOnInit(): void {
    this.loadTemplates();
  }

    loadTemplates(): void {
    this.loading = true;
    this.error = '';
    this.api.getTemplates(this.visibilityFilter, this.searchTerm).subscribe({
      next: (templates) => {
        this.templates = templates;
        this.applyFilters();  // garde les filtres locaux (catégorie/statut)
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


  // Nouvelle méthode pour changer le filtre de visibilité
  setVisibilityFilter(filter: 'ALL' | 'PRIVATE' | 'PUBLIC'): void {
    this.visibilityFilter = filter;
    this.loadTemplates();
  }

  // Ajuster la recherche : lorsque l'utilisateur tape, on relance l'API
  onSearchChange(): void {
    this.loadTemplates();
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

  // ---------- NOUVELLES MÉTHODES ----------

  archiveTemplate(id: string, event?: Event): void {
    event?.stopPropagation();
    if (!confirm('Archiver ce modèle ?')) return;
    this.api.archiveTemplate(id).subscribe({
      next: () => this.loadTemplates(),
      error: (err) => {
        console.error('Erreur archivage', err);
        this.cdr.detectChanges();
      }
    });
  }

  newVersion(id: string, event?: Event): void {
    event?.stopPropagation();
    if (!confirm('Créer une nouvelle version ? L\'ancienne sera archivée.')) return;
    this.api.newVersion(id).subscribe({
      next: (newTemplate) => this.router.navigate(['/templates', newTemplate.id]),
      error: (err) => console.error('Erreur nouvelle version', err)
    });
  }

  restoreTemplate(id: string, event?: Event): void {
    event?.stopPropagation();
    if (!confirm('Restaurer ce modèle ? Il repassera en brouillon.')) return;
    this.api.restoreTemplate(id).subscribe({
      next: () => this.loadTemplates(),
      error: (err) => {
        console.error('Erreur restauration', err);
        this.cdr.detectChanges();
      }
    });
  }

  getStatusClass(statut: string): string {
    return statut.toLowerCase();
  }
}