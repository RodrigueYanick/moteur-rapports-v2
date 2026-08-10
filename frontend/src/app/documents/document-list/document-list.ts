import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { TemplateApiService } from '../../services/template-api';
import { GeneratedDocument } from '../../models/Document.model';
import { FormsModule } from '@angular/forms';
import {
  LucideAngularModule, FileText, Search, Loader2, Download, Trash2,
  Inbox, LayoutGrid, List as ListIcon, Calendar
} from 'lucide-angular';

@Component({
  selector: 'app-document-list',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule, FormsModule],
  templateUrl: './document-list.html',
  styleUrls: ['./document-list.scss']
})
export class DocumentList implements OnInit {
  documents: GeneratedDocument[] = [];
  loading = false;
  error = '';
  searchTerm = '';
  viewMode: 'grid' | 'list' = 'grid';
  downloadingId: string | null = null;
  deletingId: string | null = null;

  readonly icons = {
    file: FileText, search: Search, loader: Loader2, download: Download,
    trash: Trash2, inbox: Inbox, grid: LayoutGrid, list: ListIcon, calendar: Calendar
  };

  private templateColors = [
    'linear-gradient(135deg, #4338ca, #6d5efc)',
    'linear-gradient(135deg, #ea580c, #f97316)',
    'linear-gradient(135deg, #047857, #10b981)',
    'linear-gradient(135deg, #7c3aed, #a78bfa)',
    'linear-gradient(135deg, #0369a1, #38bdf8)',
    'linear-gradient(135deg, #0f766e, #2dd4bf)',
  ];

  constructor(
    private api: TemplateApiService,
    private cdr: ChangeDetectorRef
  ) {}

  get filteredDocuments(): GeneratedDocument[] {
    if (!this.searchTerm.trim()) return this.documents;
    const term = this.searchTerm.toLowerCase();
    return this.documents.filter(
      (doc) =>
        doc.nom.toLowerCase().includes(term) ||
        (doc.templateNom && doc.templateNom.toLowerCase().includes(term))
    );
  }

  ngOnInit(): void {
    this.loadDocuments();
  }

  setViewMode(mode: 'grid' | 'list'): void {
    this.viewMode = mode;
  }

  loadDocuments(): void {
    this.loading = true;
    this.error = '';
    this.api.getAllDocuments().subscribe({
      next: (docs) => {
        this.documents = docs;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des documents.';
        this.loading = false;
        this.cdr.detectChanges();
        console.error(err);
      }
    });
  }

  cardBackground(doc: GeneratedDocument): string {
    // Couleur stable par templateId, pour que chaque modèle garde toujours la même couleur
    let hash = 0;
    for (const char of doc.templateId) hash = (hash + char.charCodeAt(0)) % this.templateColors.length;
    return this.templateColors[hash];
  }

  initials(nom: string): string {
    return nom
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map(w => w[0]?.toUpperCase())
      .join('');
  }

  downloadDocument(doc: GeneratedDocument, event?: Event): void {
    event?.stopPropagation();
    if (this.downloadingId) return;
    this.downloadingId = doc.id;
    this.api.generateDocument(doc.templateId, doc.donnees).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${doc.nom}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.downloadingId = null;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur génération PDF', err);
        alert('Échec de la génération du PDF. Le modèle a peut-être changé depuis la sauvegarde.');
        this.downloadingId = null;
        this.cdr.detectChanges();
      }
    });
  }

  deleteDocument(doc: GeneratedDocument, event?: Event): void {
    event?.stopPropagation();
    if (!confirm(`Supprimer "${doc.nom}" ?`)) return;
    this.deletingId = doc.id;
    this.api.deleteDocument(doc.templateId, doc.id).subscribe({
      next: () => {
        this.documents = this.documents.filter((d) => d.id !== doc.id);
        this.deletingId = null;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur lors de la suppression :', err);
        this.deletingId = null;
        this.cdr.detectChanges();
        alert('Erreur lors de la suppression du document.');
      }
    });
  }
}