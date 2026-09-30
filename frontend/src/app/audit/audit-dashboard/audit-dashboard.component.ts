import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  LucideAngularModule,
  Activity,
  FileText,
  GitBranch,
  Users,
  Search,
  RefreshCw,
  Eye,
  X,
  Filter,
  Shield,
  Download,
  CheckCircle2,
  AlertCircle
} from 'lucide-angular';
import { AuditService } from '../../services/audit.service';
import { AuditLog, AuditStats, AuditSearchFilter } from '../../models/audit.model';

@Component({
  selector: 'app-audit-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './audit-dashboard.component.html',
  styleUrls: ['./audit-dashboard.component.scss']
})
export class AuditDashboardComponent implements OnInit {
  logs: AuditLog[] = [];
  stats: AuditStats | null = null;
  loading = false;
  loadingStats = false;
  errorMsg = '';

  // Filtres
  filterAction = '';
  filterEntity = '';
  filterUser = '';
  fromDate = '';
  toDate = '';

  // Modale détails
  selectedLog: AuditLog | null = null;

  readonly icons = {
    activity: Activity,
    fileText: FileText,
    gitBranch: GitBranch,
    users: Users,
    search: Search,
    refresh: RefreshCw,
    eye: Eye,
    close: X,
    filter: Filter,
    shield: Shield,
    download: Download,
    check: CheckCircle2,
    alert: AlertCircle
  };

  readonly actionsList = [
    { label: 'Toutes les actions', value: '' },
    { label: 'Génération de document', value: 'DOCUMENT_GENERATE' },
    { label: 'Téléchargement de document', value: 'DOCUMENT_DOWNLOAD' },
    { label: 'Création de modèle', value: 'TEMPLATE_CREATE' },
    { label: 'Modification de modèle', value: 'TEMPLATE_UPDATE' },
    { label: 'Publication de modèle', value: 'TEMPLATE_PUBLISH' },
    { label: 'Soumission workflow', value: 'WORKFLOW_SUBMIT' },
    { label: 'Approbation workflow', value: 'WORKFLOW_APPROVE' },
    { label: 'Rejet workflow', value: 'WORKFLOW_REJECT' },
    { label: 'Import certificat', value: 'CERTIFICATE_UPLOAD' },
    { label: 'Suppression certificat', value: 'CERTIFICATE_DELETE' }
  ];

  constructor(
    private auditService: AuditService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadStats();
    this.loadLogs();
  }

  loadStats(): void {
    this.loadingStats = true;
    this.auditService.getStats().subscribe({
      next: (data) => {
        this.stats = data;
        this.loadingStats = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur chargement stats audit', err);
        this.loadingStats = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadLogs(): void {
    this.loading = true;
    this.errorMsg = '';

    const filter: AuditSearchFilter = {
      action: this.filterAction || undefined,
      entityName: this.filterEntity || undefined,
      performedBy: this.filterUser.trim() || undefined,
      fromDate: this.fromDate || undefined,
      toDate: this.toDate || undefined,
      size: 100
    };

    this.auditService.searchLogs(filter).subscribe({
      next: (data) => {
        this.logs = data || [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loading = false;
        this.errorMsg = 'Impossible de charger les journaux d\'audit.';
        console.error(err);
        this.cdr.detectChanges();
      }
    });
  }

  resetFilters(): void {
    this.filterAction = '';
    this.filterEntity = '';
    this.filterUser = '';
    this.fromDate = '';
    this.toDate = '';
    this.loadLogs();
  }

  openDetails(log: AuditLog): void {
    this.selectedLog = log;
  }

  closeDetails(): void {
    this.selectedLog = null;
  }

  formatJsonDetails(jsonStr?: string): string {
    if (!jsonStr) return 'Aucun détail additionnel';
    try {
      const parsed = JSON.parse(jsonStr);
      return JSON.stringify(parsed, null, 2);
    } catch {
      return jsonStr;
    }
  }

  getActionBadgeClass(action: string): string {
    if (action.includes('GENERATE') || action.includes('APPROVE') || action.includes('PUBLISH')) {
      return 'badge-success';
    }
    if (action.includes('REJECT') || action.includes('DELETE')) {
      return 'badge-danger';
    }
    if (action.includes('SUBMIT') || action.includes('UPDATE')) {
      return 'badge-warning';
    }
    return 'badge-info';
  }

  formatActionLabel(action: string): string {
    switch (action) {
      case 'DOCUMENT_GENERATE': return 'Génération PDF';
      case 'DOCUMENT_DOWNLOAD': return 'Téléchargement PDF';
      case 'TEMPLATE_CREATE': return 'Création Modèle';
      case 'TEMPLATE_UPDATE': return 'Modif Modèle';
      case 'TEMPLATE_PUBLISH': return 'Publication Modèle';
      case 'WORKFLOW_SUBMIT': return 'Soumission Revue';
      case 'WORKFLOW_APPROVE': return 'Approbation Modèle';
      case 'WORKFLOW_REJECT': return 'Rejet Modèle';
      case 'CERTIFICATE_UPLOAD': return 'Import Certificat';
      case 'CERTIFICATE_DELETE': return 'Suppr Certificat';
      default: return action;
    }
  }
}
