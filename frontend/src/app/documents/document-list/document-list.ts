import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TemplateApiService } from '../../services/template-api';
import { GeneratedDocument } from '../../models/Document.model';
import { ToastService } from '../../shared/services/toast.service';
import { PdfViewerModalComponent } from '../../shared/components/pdf-viewer-modal/pdf-viewer-modal.component';
import { DocumentEmailModalComponent } from '../../shared/components/document-email-modal/document-email-modal.component';
import {
  LucideAngularModule,
  FileText,
  Search,
  Loader2,
  Download,
  Trash2,
  Inbox,
  LayoutGrid,
  List as ListIcon,
  Calendar,
  FileSpreadsheet,
  Eye,
  Mail,
  Send,
} from 'lucide-angular';

@Component({
  selector: 'app-document-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    LucideAngularModule,
    FormsModule,
    PdfViewerModalComponent,
    DocumentEmailModalComponent,
  ],
  templateUrl: './document-list.html',
  styleUrls: ['./document-list.scss'],
})
export class DocumentList implements OnInit {
  documents: GeneratedDocument[] = [];
  loading = false;
  error = '';
  searchTerm = '';
  viewMode: 'grid' | 'list' = 'grid';
  downloadingId: string | null = null;
  downloadingExcelId: string | null = null;
  deletingId: string | null = null;
  visibilityFilter: 'ALL' | 'PUBLIC' | 'PRIVATE' = 'ALL';

  // Modal Visionneuse PDF In-App
  previewModalVisible = false;
  previewTitle = '';
  previewPdfBlob: Blob | null = null;
  previewingId: string | null = null;

  // Modal Expédition Email
  emailModalVisible = false;
  activeDocForEmail: GeneratedDocument | null = null;

  readonly icons = {
    file: FileText,
    search: Search,
    loader: Loader2,
    download: Download,
    trash: Trash2,
    inbox: Inbox,
    grid: LayoutGrid,
    list: ListIcon,
    calendar: Calendar,
    spreadsheet: FileSpreadsheet,
    eye: Eye,
    mail: Mail,
    send: Send,
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
    private toast: ToastService,
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
    this.api.getAllDocuments(this.visibilityFilter, this.searchTerm).subscribe({
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
      },
    });
  }

  setVisibilityFilter(filter: 'ALL' | 'PRIVATE' | 'PUBLIC'): void {
    this.visibilityFilter = filter;
    this.loadDocuments();
  }

  onSearchChange(): void {
    this.loadDocuments();
  }

  cardBackground(doc: GeneratedDocument): string {
    let hash = 0;
    const str = doc.templateNom || doc.nom || doc.templateId || '';
    for (let i = 0; i < str.length; i++) {
      hash = (hash << 5) - hash + str.charCodeAt(i);
      hash |= 0;
    }
    const idx = Math.abs(hash) % this.templateColors.length;
    return this.templateColors[idx];
  }

  initials(nom: string): string {
    return nom
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((w) => w[0]?.toUpperCase() || '')
      .join('');
  }

  previewDocument(doc: GeneratedDocument, event?: Event): void {
    event?.stopPropagation();
    if (this.previewingId) return;
    this.previewingId = doc.id;
    this.api.generateDocument(doc.templateId, doc.donnees).subscribe({
      next: (blob: Blob) => {
        this.previewPdfBlob = blob;
        this.previewTitle = doc.nom;
        this.activeDocForEmail = doc;
        this.previewModalVisible = true;
        this.previewingId = null;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur génération PDF pour aperçu', err);
        this.previewingId = null;
        this.toast.error("Impossible de générer l'aperçu PDF du document.", "Erreur d'aperçu");
        this.cdr.detectChanges();
      },
    });
  }

  openEmailForDocument(doc: GeneratedDocument, event?: Event): void {
    event?.stopPropagation();
    this.activeDocForEmail = doc;
    this.emailModalVisible = true;
  }

  onPdfViewerRequestEmail(): void {
    this.previewModalVisible = false;
    this.emailModalVisible = true;
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
        this.toast.success(`Le document « ${doc.nom} » a été téléchargé en PDF.`, 'Téléchargement terminé');
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur génération PDF', err);
        alert('Échec de la génération du PDF. Le modèle a peut-être changé depuis la sauvegarde.');
        this.downloadingId = null;
        this.toast.error('Échec de la génération du PDF. Le modèle a peut-être changé depuis la sauvegarde.', 'Erreur');
        this.cdr.detectChanges();
      },
    });
  }

  downloadDocumentExcel(doc: GeneratedDocument, event?: Event): void {
    event?.stopPropagation();
    if (this.downloadingExcelId) return;
    this.downloadingExcelId = doc.id;
    this.api.exportDocumentExcel(doc.templateId, doc.id).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${doc.nom}.xlsx`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.downloadingExcelId = null;
        this.toast.success(`Le classeur Excel « ${doc.nom} » a été téléchargé.`, 'Export Excel');
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur export Excel', err);
        alert('Échec de l\'export Excel. Vérifiez les données du modèle.');
        this.downloadingExcelId = null;
        this.toast.error("Échec de l'export Excel. Vérifiez les données du modèle.", 'Erreur export');
        this.cdr.detectChanges();
      },
    });
  }

  deleteDocument(doc: GeneratedDocument, event?: Event): void {
    event?.stopPropagation();
    if (this.deletingId) return;
    if (!confirm(`Supprimer définitivement « ${doc.nom} » ?`)) return;
    this.deletingId = doc.id;
    this.api.deleteDocument(doc.templateId, doc.id).subscribe({
      next: () => {
        this.documents = this.documents.filter((d) => d.id !== doc.id);
        this.deletingId = null;
        this.toast.info(`Le document « ${doc.nom} » a été supprimé.`, 'Document supprimé');
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur lors de la suppression :', err);
        this.deletingId = null;
        this.toast.error('Erreur lors de la suppression du document.', 'Erreur');
        this.cdr.detectChanges();
        alert('Erreur lors de la suppression du document.');
      },
    });
  }
}