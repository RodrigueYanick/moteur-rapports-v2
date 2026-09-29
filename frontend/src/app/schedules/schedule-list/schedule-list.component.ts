import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  LucideAngularModule,
  Calendar,
  Clock,
  Play,
  History,
  Plus,
  Trash2,
  Edit,
  CheckCircle2,
  XCircle,
  AlertCircle,
  RefreshCw,
  Mail,
  Send,
  FileText,
  FileSpreadsheet,
  Image,
  Database
} from 'lucide-angular';
import {
  ScheduledJobResponse,
  ScheduledJobRequest,
  ScheduledJobExecutionResponse,
  ReportExportFormat
} from '../../models/scheduled-job.model';
import { ScheduledJobService } from '../../services/scheduled-job.service';
import { TemplateApiService } from '../../services/template-api';
import { Template } from '../../models/template.model';

@Component({
  selector: 'app-schedule-list',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './schedule-list.component.html',
  styleUrls: ['./schedule-list.component.scss']
})
export class ScheduleListComponent implements OnInit {
  jobs: ScheduledJobResponse[] = [];
  templates: Template[] = [];
  loading = false;
  runningJobId: string | null = null;
  errorMsg = '';
  successMsg = '';

  // Modal Création / Édition
  showEditModal = false;
  isEditing = false;
  editingJobId: string | null = null;
  formData: {
    nom: string;
    description: string;
    templateId: string;
    cronExpression: string;
    timezone: string;
    format: ReportExportFormat;
    emailsInput: string;
    webhookUrl: string;
    parametresJson: string;
    actif: boolean;
  } = this.getEmptyForm();

  // Modal Historique
  showHistoryModal = false;
  selectedJobForHistory: ScheduledJobResponse | null = null;
  executions: ScheduledJobExecutionResponse[] = [];
  loadingHistory = false;

  readonly icons = {
    calendar: Calendar,
    clock: Clock,
    play: Play,
    history: History,
    plus: Plus,
    trash: Trash2,
    edit: Edit,
    check: CheckCircle2,
    error: XCircle,
    alert: AlertCircle,
    refresh: RefreshCw,
    mail: Mail,
    send: Send,
    fileText: FileText,
    excel: FileSpreadsheet,
    image: Image,
    database: Database
  };

  readonly cronPresets = [
    { label: 'Tous les jours à 08h00', value: '0 0 8 * * *' },
    { label: 'Tous les lundis à 08h00', value: '0 0 8 * * MON' },
    { label: 'Tous les vendredis à 18h00', value: '0 0 18 * * FRI' },
    { label: 'Premier jour du mois à 06h00', value: '0 0 6 1 * *' },
    { label: 'Toutes les heures', value: '0 0 * * * *' }
  ];

  constructor(
    private scheduleService: ScheduledJobService,
    private templateService: TemplateApiService
  ) {}

  ngOnInit(): void {
    this.loadJobs();
    this.loadTemplates();
  }

  loadJobs(): void {
    this.loading = true;
    this.errorMsg = '';
    this.scheduleService.getAll().subscribe({
      next: (res) => {
        this.jobs = res || [];
        this.loading = false;
      },
      error: (err) => {
        this.errorMsg = 'Impossible de charger les plannings : ' + (err.error?.message || err.message);
        this.loading = false;
      }
    });
  }

  loadTemplates(): void {
    this.templateService.getTemplates().subscribe({
      next: (res) => {
        this.templates = res || [];
      },
      error: (err) => console.error('Erreur chargement templates', err)
    });
  }

  openCreateModal(): void {
    this.isEditing = false;
    this.editingJobId = null;
    this.formData = this.getEmptyForm();
    if (this.templates.length > 0) {
      this.formData.templateId = this.templates[0].id;
    }
    this.showEditModal = true;
  }

  openEditModal(job: ScheduledJobResponse): void {
    this.isEditing = true;
    this.editingJobId = job.id;
    this.formData = {
      nom: job.nom,
      description: job.description || '',
      templateId: job.templateId,
      cronExpression: job.cronExpression,
      timezone: job.timezone || 'Europe/Paris',
      format: job.format,
      emailsInput: (job.emailsDestinataires || []).join(', '),
      webhookUrl: job.webhookUrl || '',
      parametresJson: job.parametresJson || '',
      actif: job.actif
    };
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.editingJobId = null;
  }

  applyPreset(presetValue: string): void {
    this.formData.cronExpression = presetValue;
  }

  saveJob(): void {
    if (!this.formData.nom || !this.formData.templateId || !this.formData.cronExpression) {
      this.errorMsg = 'Le nom, le modèle et l\'expression CRON sont obligatoires.';
      return;
    }

    const emails = this.formData.emailsInput
      ? this.formData.emailsInput.split(',').map(e => e.trim()).filter(e => e.length > 0)
      : [];

    const request: ScheduledJobRequest = {
      nom: this.formData.nom,
      description: this.formData.description,
      templateId: this.formData.templateId,
      cronExpression: this.formData.cronExpression,
      timezone: this.formData.timezone,
      format: this.formData.format,
      emailsDestinataires: emails,
      webhookUrl: this.formData.webhookUrl,
      parametresJson: this.formData.parametresJson,
      actif: this.formData.actif
    };

    this.loading = true;
    const obs$ = this.isEditing && this.editingJobId
      ? this.scheduleService.update(this.editingJobId, request)
      : this.scheduleService.create(request);

    obs$.subscribe({
      next: () => {
        this.successMsg = this.isEditing ? 'Tâche mise à jour avec succès' : 'Tâche planifiée avec succès';
        setTimeout(() => this.successMsg = '', 3500);
        this.closeEditModal();
        this.loadJobs();
      },
      error: (err) => {
        this.errorMsg = 'Erreur lors de l\'enregistrement : ' + (err.error?.message || err.message);
        this.loading = false;
      }
    });
  }

  runJobNow(job: ScheduledJobResponse): void {
    if (this.runningJobId) return;
    this.runningJobId = job.id;
    this.scheduleService.runNow(job.id).subscribe({
      next: (res) => {
        this.runningJobId = null;
        this.successMsg = `Exécution immédiate terminée (${res.statut}) en ${res.dureeMs || 0} ms`;
        setTimeout(() => this.successMsg = '', 4000);
        this.loadJobs();
      },
      error: (err) => {
        this.runningJobId = null;
        this.errorMsg = 'Échec de l\'exécution : ' + (err.error?.message || err.message);
      }
    });
  }

  openHistoryModal(job: ScheduledJobResponse): void {
    this.selectedJobForHistory = job;
    this.showHistoryModal = true;
    this.loadingHistory = true;
    this.executions = [];

    this.scheduleService.getExecutions(job.id).subscribe({
      next: (res) => {
        this.executions = res || [];
        this.loadingHistory = false;
      },
      error: (err) => {
        this.errorMsg = 'Impossible de charger l\'historique : ' + (err.error?.message || err.message);
        this.loadingHistory = false;
      }
    });
  }

  closeHistoryModal(): void {
    this.showHistoryModal = false;
    this.selectedJobForHistory = null;
    this.executions = [];
  }

  deleteJob(job: ScheduledJobResponse): void {
    if (!confirm(`Confirmez-vous la suppression de la tâche planifiée "${job.nom}" ?`)) return;

    this.scheduleService.delete(job.id).subscribe({
      next: () => {
        this.successMsg = 'Tâche planifiée supprimée';
        setTimeout(() => this.successMsg = '', 3000);
        this.loadJobs();
      },
      error: (err) => {
        this.errorMsg = 'Erreur lors de la suppression : ' + (err.error?.message || err.message);
      }
    });
  }

  getCronDescription(cron: string): string {
    if (cron === '0 0 8 * * *') return 'Tous les jours à 08h00';
    if (cron === '0 0 8 * * MON') return 'Tous les lundis à 08h00';
    if (cron === '0 0 18 * * FRI') return 'Tous les vendredis à 18h00';
    if (cron === '0 0 6 1 * *') return 'Le 1er du mois à 06h00';
    if (cron === '0 0 * * * *') return 'Toutes les heures';
    return cron;
  }

  private getEmptyForm() {
    return {
      nom: '',
      description: '',
      templateId: '',
      cronExpression: '0 0 8 * * *',
      timezone: 'Europe/Paris',
      format: 'PDF' as ReportExportFormat,
      emailsInput: '',
      webhookUrl: '',
      parametresJson: '',
      actif: true
    };
  }
}
