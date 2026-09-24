import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { TemplateApiService } from '../../services/template-api';
import { Template } from '../../models/template.model';
import {
  BatchResponse,
  BatchItemResponse,
  BatchCreateRequest,
  WebhookTestResponse,
} from '../../models/batch.model';
import {
  LucideAngularModule,
  Layers,
  Plus,
  RefreshCw,
  Download,
  CheckCircle,
  AlertTriangle,
  XCircle,
  Clock,
  Send,
  Play,
  ChevronDown,
  ChevronRight,
  Archive,
  Loader2,
} from 'lucide-angular';
import { Subscription, interval } from 'rxjs';
import { ExcelImportModalComponent, BatchImportItem } from '../../shared/components/excel-import-modal/excel-import-modal.component';
import { Variable } from '../../models/variable.model';

@Component({
  selector: 'app-batch-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, LucideAngularModule, ExcelImportModalComponent],
  templateUrl: './batch-list.html',
  styleUrls: ['./batch-list.scss'],
})
export class BatchListComponent implements OnInit, OnDestroy {
  batches: BatchResponse[] = [];
  templates: Template[] = [];
  loading = false;
  error: string | null = null;

  // Filtre
  selectedTemplateFilter: string = '';

  // Détails étendus par lot (accordéon)
  expandedBatchId: string | null = null;
  batchItemsMap: Record<string, BatchItemResponse[]> = {};
  loadingItems: Record<string, boolean> = {};

  // Modal Nouveau Lot
  showCreateModal = false;
  creatingBatch = false;
  createError: string | null = null;
  selectedTemplateId: string = '';
  jsonInput: string = '';
  webhookUrlInput: string = '';
  webhookSecretInput: string = '';

  // Assistant Import Excel / CSV
  showExcelImportModal = false;
  selectedTemplateVariables: Variable[] = [];
  excelImportSuccessMessage: string | null = null;

  // Test Webhook
  testingWebhook = false;
  webhookTestResult: WebhookTestResponse | null = null;

  // Actions de téléchargement ZIP
  downloadingZipId: string | null = null;
  retryingBatchId: string | null = null;

  private pollSubscription?: Subscription;

  readonly icons = {
    layers: Layers,
    plus: Plus,
    refresh: RefreshCw,
    download: Download,
    check: CheckCircle,
    warning: AlertTriangle,
    error: XCircle,
    clock: Clock,
    send: Send,
    play: Play,
    chevronDown: ChevronDown,
    chevronRight: ChevronRight,
    archive: Archive,
    loader: Loader2,
  };

  constructor(
    private api: TemplateApiService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      if (params['templateId']) {
        this.selectedTemplateFilter = params['templateId'];
        this.selectedTemplateId = params['templateId'];
      }
      if (params['new'] === 'true') {
        this.openCreateModal();
      }
    });

    this.loadTemplates();
    this.loadBatches();

    // Polling toutes les 4 secondes pour rafraîchir les lots en cours
    this.pollSubscription = interval(4000).subscribe(() => {
      if (this.batches.some((b) => b.statut === 'EN_COURS' || b.statut === 'EN_ATTENTE')) {
        this.loadBatches(true);
      }
    });
  }

  ngOnDestroy(): void {
    if (this.pollSubscription) {
      this.pollSubscription.unsubscribe();
    }
  }

  loadTemplates(): void {
    this.api.getTemplates().subscribe({
      next: (data) => {
        this.templates = data.filter((t) => t.statut === 'PUBLIE');
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erreur chargement modèles', err),
    });
  }

  loadBatches(silent = false): void {
    if (!silent) this.loading = true;
    this.error = null;

    this.api.listBatches(this.selectedTemplateFilter || undefined).subscribe({
      next: (data) => {
        this.batches = data;
        this.loading = false;

        // Si le lot déplié est en cours, recharger ses items
        if (this.expandedBatchId) {
          const currentBatch = data.find((b) => b.id === this.expandedBatchId);
          if (currentBatch && (currentBatch.statut === 'EN_COURS' || currentBatch.statut === 'EN_ATTENTE')) {
            this.loadBatchItems(this.expandedBatchId);
          }
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.loading = false;
        this.error = 'Impossible de charger la liste des traitements par lot';
        this.cdr.detectChanges();
      },
    });
  }

  onFilterChange(): void {
    this.loadBatches();
  }

  toggleExpand(batchId: string): void {
    if (this.expandedBatchId === batchId) {
      this.expandedBatchId = null;
    } else {
      this.expandedBatchId = batchId;
      if (!this.batchItemsMap[batchId]) {
        this.loadBatchItems(batchId);
      }
    }
  }

  loadBatchItems(batchId: string): void {
    this.loadingItems[batchId] = true;
    this.api.getBatchItems(batchId).subscribe({
      next: (items) => {
        this.batchItemsMap[batchId] = items;
        this.loadingItems[batchId] = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur chargement items du lot', err);
        this.loadingItems[batchId] = false;
        this.cdr.detectChanges();
      },
    });
  }

  openCreateModal(): void {
    this.showCreateModal = true;
    this.createError = null;
    this.webhookTestResult = null;
    this.excelImportSuccessMessage = null;
    if (!this.selectedTemplateId && this.templates.length > 0) {
      this.selectedTemplateId = this.templates[0].id;
    }
    if (this.selectedTemplateId) {
      this.loadTemplateVariables(this.selectedTemplateId);
    }
    if (!this.jsonInput) {
      this.loadSampleJson();
    }
    if (!this.selectedTemplateId && this.templates.length > 0) {
      this.selectedTemplateId = this.templates[0].id;
    }
  }

  closeCreateModal(): void {
    this.showCreateModal = false;
    this.showExcelImportModal = false;
  }

  onTemplateChanged(templateId: string): void {
    this.selectedTemplateId = templateId;
    if (templateId) {
      this.loadTemplateVariables(templateId);
    }
  }

  loadTemplateVariables(templateId: string): void {
    this.api.getVariables(templateId).subscribe({
      next: (vars) => {
        this.selectedTemplateVariables = vars;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.warn('Impossible de charger les variables du template', err);
        this.selectedTemplateVariables = [];
      },
    });
  }

  openExcelImportModal(): void {
    if (!this.selectedTemplateId) {
      this.createError = 'Veuillez sélectionner un modèle de rapport avant d\'importer un fichier Excel.';
      return;
    }
    if (this.selectedTemplateVariables.length === 0) {
      this.loadTemplateVariables(this.selectedTemplateId);
    }
    this.showExcelImportModal = true;
  }

  onExcelBatchImported(items: BatchImportItem[]): void {
    this.jsonInput = JSON.stringify(items, null, 2);
    this.showExcelImportModal = false;
    this.excelImportSuccessMessage = `✓ ${items.length} rapport${items.length > 1 ? 's' : ''} importé${items.length > 1 ? 's' : ''} avec succès depuis Excel !`;
    this.cdr.detectChanges();
  }

  loadSampleJson(): void {
    const sample = [
      {
        customId: 'FACT-2026-001',
        data: {
          clientNom: 'Société Alpha',
          montantTotal: 1250.5,
          statutFacture: 'PAYEE',
        },
      },
      {
        customId: 'FACT-2026-002',
        data: {
          clientNom: 'Entreprise Bêta',
          montantTotal: 480.0,
          statutFacture: 'EN_ATTENTE',
        },
      },
      {
        customId: 'FACT-2026-003',
        data: {
          clientNom: 'Groupe Gamma',
          montantTotal: 3420.9,
          statutFacture: 'PAYEE',
        },
      },
    ];
    this.jsonInput = JSON.stringify(sample, null, 2);
  }

  get parsedItemsCount(): number | null {
    if (!this.jsonInput || !this.jsonInput.trim()) return null;
    try {
      const parsed = JSON.parse(this.jsonInput);
      if (Array.isArray(parsed)) return parsed.length;
      return null;
    } catch {
      return null;
    }
  }

  testWebhookConnection(): void {
    if (!this.webhookUrlInput || !this.webhookUrlInput.trim()) return;
    this.testingWebhook = true;
    this.webhookTestResult = null;

    this.api
      .testWebhook({
        url: this.webhookUrlInput.trim(),
        secret: this.webhookSecretInput.trim() || undefined,
      })
      .subscribe({
        next: (res) => {
          this.webhookTestResult = res;
          this.testingWebhook = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.webhookTestResult = {
            succes: false,
            statusCode: 0,
            message: 'Erreur réseau lors du test du webhook',
            tempsReponseMs: 0,
          };
          this.testingWebhook = false;
          this.cdr.detectChanges();
        },
      });
  }

  submitBatch(): void {
    this.createError = null;

    if (!this.selectedTemplateId) {
      this.createError = 'Veuillez sélectionner un modèle de rapport';
      return;
    }

    let parsedItems: any[];
    try {
      parsedItems = JSON.parse(this.jsonInput);
      if (!Array.isArray(parsedItems) || parsedItems.length === 0) {
        this.createError = 'Le JSON doit être un tableau non vide de données';
        return;
      }
    } catch (e) {
      this.createError = 'Format JSON invalide. Veuillez vérifier la syntaxe.';
      return;
    }

    const payload: BatchCreateRequest = {
      templateId: this.selectedTemplateId,
      items: parsedItems.map((item, idx) => ({
        customId: item.customId || `item-${idx + 1}`,
        data: item.data || item,
      })),
      webhookUrl: this.webhookUrlInput.trim() || undefined,
      webhookSecret: this.webhookSecretInput.trim() || undefined,
    };

    this.creatingBatch = true;
    this.api.createBatch(this.selectedTemplateId, payload).subscribe({
      next: (created) => {
        this.creatingBatch = false;
        this.showCreateModal = false;
        this.loadBatches();
      },
      error: (err) => {
        this.creatingBatch = false;
        this.createError = err.error?.message || 'Échec lors de la création du lot';
        this.cdr.detectChanges();
      },
    });
  }

  downloadZip(batch: BatchResponse): void {
    this.downloadingZipId = batch.id;
    this.api.downloadBatchZip(batch.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `lot_${batch.templateNom || 'rapports'}_${batch.id.substring(0, 8)}.zip`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.downloadingZipId = null;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Erreur téléchargement ZIP', err);
        alert("Impossible de télécharger l'archive ZIP.");
        this.downloadingZipId = null;
        this.cdr.detectChanges();
      },
    });
  }

  retryFailed(batchId: string): void {
    this.retryingBatchId = batchId;
    this.api.retryFailedBatch(batchId).subscribe({
      next: (updated) => {
        this.retryingBatchId = null;
        this.loadBatches();
      },
      error: (err) => {
        console.error('Erreur relance des échecs', err);
        alert(err.error?.message || 'Impossible de relancer les échecs.');
        this.retryingBatchId = null;
        this.cdr.detectChanges();
      },
    });
  }

  // Statistiques calculées
  get totalBatchesCount(): number {
    return this.batches.length;
  }

  get runningBatchesCount(): number {
    return this.batches.filter((b) => b.statut === 'EN_COURS' || b.statut === 'EN_ATTENTE').length;
  }

  get globalSuccessRate(): number {
    const totalReports = this.batches.reduce((sum, b) => sum + b.totalItems, 0);
    const totalSuccess = this.batches.reduce((sum, b) => sum + b.successCount, 0);
    if (totalReports === 0) return 100;
    return Math.round((totalSuccess / totalReports) * 100);
  }
}

