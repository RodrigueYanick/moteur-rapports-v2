import { Component, EventEmitter, Input, Output, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  LucideAngularModule,
  Sparkles,
  Wand2,
  X,
  FileText,
  CheckCircle2,
  AlertCircle,
  ArrowRight,
  Loader2,
} from 'lucide-angular';
import { AiAssistantService } from '../../../services/ai-assistant.service';
import {
  AiTemplateGenerationResponse,
  AiStatusResponse,
} from '../../../models/ai.model';

@Component({
  selector: 'app-ai-template-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './ai-template-modal.component.html',
  styleUrls: ['./ai-template-modal.component.scss'],
})
export class AiTemplateModalComponent implements OnInit {
  @Input() visible = false;
  @Output() close = new EventEmitter<void>();
  @Output() created = new EventEmitter<AiTemplateGenerationResponse>();

  prompt = '';
  nom = '';
  categorie = 'VENTES';
  formatPapier = 'A4';

  loading = false;
  error: string | null = null;
  aiStatus: AiStatusResponse | null = null;

  readonly icons = {
    sparkles: Sparkles,
    wand: Wand2,
    x: X,
    file: FileText,
    check: CheckCircle2,
    alert: AlertCircle,
    arrowRight: ArrowRight,
    loader: Loader2,
  };

  readonly categories = [
    'VENTES',
    'ACHATS',
    'FINANCE',
    'RH',
    'LOGISTIQUE',
    'STOCK',
    'PRODUCTION',
    'ADMINISTRATION',
    'AUTRES',
  ];

  readonly suggestions = [
    {
      title: 'Devis Artisan BTP',
      prompt:
        'Modèle de devis pour artisan du bâtiment avec logo en en-tête, tableau détaillé de prestations (quantité, prix unitaire, TVA 10%, total), cadre acompte 30% et coordonnées bancaires.',
      categorie: 'VENTES',
      nom: 'Devis Travaux BTP',
    },
    {
      title: 'Fiche de Paie',
      prompt:
        'Bulletin de salaire mensuel avec informations employé/employeur, tableau détaillé des cotisations salariales et patronales, cumul annuel et net à payer mis en valeur.',
      categorie: 'RH',
      nom: 'Fiche de Paie Cadre',
    },
    {
      title: 'Rapport d\'Audit Sécurité',
      prompt:
        'Rapport d\'audit de sécurité informatique avec cartouche de métadonnées, score de conformité global, tableau des vulnérabilités critiques et matrice de recommandations.',
      categorie: 'ADMINISTRATION',
      nom: 'Rapport Audit Sécurité',
    },
    {
      title: 'Bon de Livraison',
      prompt:
        'Bon de livraison logistique avec numéro de commande, adresse expéditeur et destinataire, tableau récapitulatif des colis et zone pour signature du destinataire.',
      categorie: 'LOGISTIQUE',
      nom: 'Bon de Livraison Express',
    },
  ];

  constructor(
    private aiService: AiAssistantService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.checkStatus();
  }

  checkStatus(): void {
    this.aiService.getStatus().subscribe({
      next: (status) => {
        this.aiStatus = status;
        this.cdr.detectChanges();
      },
      error: () => {
        // Fallback silencieux en cas d'indisponibilité du statut
        this.aiStatus = {
          enabled: true,
          available: false,
          provider: 'MOCK',
          model: 'Heuristic-Engine',
        };
        this.cdr.detectChanges();
      },
    });
  }

  applySuggestion(suggestion: (typeof this.suggestions)[0]): void {
    this.prompt = suggestion.prompt;
    this.nom = suggestion.nom;
    this.categorie = suggestion.categorie;
  }

  generate(): void {
    if (!this.prompt.trim()) {
      this.error = 'Veuillez saisir une description de votre modèle.';
      return;
    }

    this.loading = true;
    this.error = null;

    this.aiService
      .generateTemplate({
        prompt: this.prompt.trim(),
        nom: this.nom.trim() || undefined,
        categorie: this.categorie,
        formatPapier: this.formatPapier,
      })
      .subscribe({
        next: (response) => {
          this.loading = false;
          this.created.emit(response);
          this.closeModal();
          this.router.navigate(['/templates', response.templateId]);
        },
        error: (err) => {
          this.loading = false;
          this.error =
            err.error?.message ||
            'Une erreur est survenue lors de la génération du modèle. Veuillez réessayer.';
          this.cdr.detectChanges();
        },
      });
  }

  closeModal(): void {
    this.error = null;
    this.close.emit();
  }
}
