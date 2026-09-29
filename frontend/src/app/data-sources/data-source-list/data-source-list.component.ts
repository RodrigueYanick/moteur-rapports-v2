import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  LucideAngularModule,
  Database,
  Server,
  Globe,
  Plus,
  Trash2,
  Edit,
  Play,
  CheckCircle2,
  XCircle,
  AlertCircle,
  RefreshCw,
  Key,
  ShieldCheck
} from 'lucide-angular';
import { DataSourceConfig, DataSourceTestResult } from '../../models/data-source.model';
import { DataSourceService } from '../../services/data-source.service';

@Component({
  selector: 'app-data-source-list',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './data-source-list.component.html',
  styleUrls: ['./data-source-list.component.scss']
})
export class DataSourceListComponent implements OnInit {
  dataSources: DataSourceConfig[] = [];
  loading = false;
  errorMsg = '';
  successMsg = '';

  // Modal création / modification
  showModal = false;
  isEditing = false;
  currentSource: Partial<DataSourceConfig> = this.getEmptySource();

  // Test de connexion
  testing = false;
  testResult: DataSourceTestResult | null = null;

  readonly icons = {
    database: Database,
    server: Server,
    globe: Globe,
    plus: Plus,
    trash: Trash2,
    edit: Edit,
    play: Play,
    check: CheckCircle2,
    error: XCircle,
    alert: AlertCircle,
    refresh: RefreshCw,
    key: Key,
    shield: ShieldCheck
  };

  constructor(private dataSourceService: DataSourceService) {}

  ngOnInit(): void {
    this.loadDataSources();
  }

  loadDataSources(): void {
    this.loading = true;
    this.errorMsg = '';
    this.dataSourceService.getAll().subscribe({
      next: (res) => {
        this.dataSources = res || [];
        this.loading = false;
      },
      error: (err) => {
        this.errorMsg = 'Impossible de charger les sources de données : ' + (err.error?.message || err.message);
        this.loading = false;
      }
    });
  }

  openCreateModal(): void {
    this.isEditing = false;
    this.currentSource = this.getEmptySource();
    this.testResult = null;
    this.showModal = true;
  }

  openEditModal(source: DataSourceConfig): void {
    this.isEditing = true;
    this.currentSource = { ...source };
    this.testResult = null;
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.testResult = null;
  }

  onTypeChange(): void {
    if (this.currentSource.type === 'POSTGRESQL' && !this.currentSource.port) {
      this.currentSource.port = 5432;
    } else if (this.currentSource.type === 'MYSQL' && !this.currentSource.port) {
      this.currentSource.port = 3306;
    }
  }

  testConnection(): void {
    this.testing = true;
    this.testResult = null;
    this.dataSourceService.testConnection(this.currentSource).subscribe({
      next: (res) => {
        this.testResult = res;
        this.testing = false;
      },
      error: (err) => {
        this.testResult = {
          succes: false,
          message: err.error?.message || 'Erreur lors du test de connexion'
        };
        this.testing = false;
      }
    });
  }

  saveSource(): void {
    if (!this.currentSource.nom || !this.currentSource.urlOuHote) {
      this.errorMsg = 'Veuillez remplir les champs obligatoires (nom, hôte/URL)';
      return;
    }

    this.loading = true;
    const obs$ = this.isEditing && this.currentSource.id
      ? this.dataSourceService.update(this.currentSource.id, this.currentSource)
      : this.dataSourceService.create(this.currentSource);

    obs$.subscribe({
      next: () => {
        this.successMsg = this.isEditing ? 'Source modifiée avec succès' : 'Source créée avec succès';
        setTimeout(() => this.successMsg = '', 3000);
        this.closeModal();
        this.loadDataSources();
      },
      error: (err) => {
        this.errorMsg = 'Erreur lors de la sauvegarde : ' + (err.error?.message || err.message);
        this.loading = false;
      }
    });
  }

  deleteSource(source: DataSourceConfig): void {
    if (!source.id) return;
    if (!confirm(`Confirmez-vous la suppression de la source de données "${source.nom}" ?`)) return;

    this.dataSourceService.delete(source.id).subscribe({
      next: () => {
        this.successMsg = 'Source supprimée avec succès';
        setTimeout(() => this.successMsg = '', 3000);
        this.loadDataSources();
      },
      error: (err) => {
        this.errorMsg = 'Erreur lors de la suppression : ' + (err.error?.message || err.message);
      }
    });
  }

  private getEmptySource(): Partial<DataSourceConfig> {
    return {
      nom: '',
      type: 'POSTGRESQL',
      urlOuHote: '',
      port: 5432,
      nomBase: '',
      nomUtilisateur: '',
      motDePasse: '',
      authType: 'AUCUNE',
      timeoutSecondes: 10,
      actif: true
    };
  }
}
