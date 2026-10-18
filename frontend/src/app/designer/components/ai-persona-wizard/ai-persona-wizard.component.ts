import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AiPersonaService, AiPersonaGenerationResponse } from '../../../services/ai-persona.service';
import { ProfileService } from '../../../services/profile.service';

export interface PersonaOption {
  code: string;
  titre: string;
  badge: string;
  icon: string;
  description: string;
  paperFormat: string;
  samplePrompts: string[];
}

@Component({
  selector: 'app-ai-persona-wizard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './ai-persona-wizard.component.html',
  styleUrls: ['./ai-persona-wizard.component.scss']
})
export class AiPersonaWizardComponent {
  @Input() isOpen = false;
  @Output() close = new EventEmitter<void>();
  @Output() templateGenerated = new EventEmitter<AiPersonaGenerationResponse>();

  currentStep = signal<1 | 2 | 3>(1);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  personas: PersonaOption[] = [
    {
      code: 'CV_CANADIAN',
      titre: 'CV Standard Canadien',
      badge: 'Canada / Québec',
      icon: '🍁',
      description: 'Format Letter, zéro photo, conformité stricte aux lois fédérales anti-discrimination.',
      paperFormat: 'Letter',
      samplePrompts: [
        'Ingénieur logiciel senior avec 6 ans d\'expérience Cloud & DevOps à Montréal',
        'Gestionnaire de projet bilingue certifié PMP avec expérience dans le secteur bancaire'
      ]
    },
    {
      code: 'CV_ATS',
      titre: 'CV ATS Haute Performance',
      badge: 'Recrutement US / Tech',
      icon: '🤖',
      description: 'Mise en page 100% linéaire optimisée pour le filtrage automatisé des robots ATS.',
      paperFormat: 'Letter',
      samplePrompts: [
        'Fullstack Developer (Java, Spring, Angular, Docker, Kubernetes)',
        'Data Scientist spécialisé en modèles LLM et traitement du langage naturel'
      ]
    },
    {
      code: 'STAGE_REPORT',
      titre: 'Rapport de Stage / PFE',
      badge: 'Académique / Université',
      icon: '🎓',
      description: 'Page de garde universitaire officielle, mentions tuteurs, entreprise et structure équilibrée.',
      paperFormat: 'A4',
      samplePrompts: [
        'Rapport de stage de fin d\'études en cybersécurité chez un grand opérateur télécom',
        'Mémoire de recherche appliquée en intelligence artificielle et vision par ordinateur'
      ]
    },
    {
      code: 'FREELANCE_INVOICE',
      titre: 'Facture Freelance Légale',
      badge: 'Comptabilité / Indépendant',
      icon: '💼',
      description: 'Mentions SIRET/TVA, ventilation HT/TTC, coordonnées bancaires IBAN et pénalités de retard.',
      paperFormat: 'A4',
      samplePrompts: [
        'Prestation de conseil DevOps de 10 jours à 650€ / jour avec TVA 20%',
        'Développement d\'une application web sur mesure forfait 4 500€ HT'
      ]
    }
  ];

  selectedPersona: PersonaOption = this.personas[0];
  userPrompt = '';
  targetLanguage = 'fr';
  useUniversalProfile = true;

  generatedResponse: AiPersonaGenerationResponse | null = null;

  constructor(
    private aiPersonaService: AiPersonaService,
    private profileService: ProfileService
  ) {}

  selectPersona(persona: PersonaOption): void {
    this.selectedPersona = persona;
    if (persona.samplePrompts.length > 0 && !this.userPrompt) {
      this.userPrompt = persona.samplePrompts[0];
    }
  }

  useSamplePrompt(prompt: string): void {
    this.userPrompt = prompt;
  }

  goToStep(step: 1 | 2 | 3): void {
    this.currentStep.set(step);
  }

  generate(): void {
    if (!this.userPrompt.trim()) {
      this.errorMessage.set('Veuillez décrire votre besoin ou poste cible.');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.aiPersonaService.generatePersonaTemplate({
      personaType: this.selectedPersona.code,
      userPrompt: this.userPrompt.trim(),
      targetLanguage: this.targetLanguage,
      includeMockData: true
    }).subscribe({
      next: (res) => {
        this.generatedResponse = res;

        // Si l'utilisateur a activé le pré-remplissage via son profil universel
        if (this.useUniversalProfile) {
          this.injectProfileDataIntoSample(res);
        }

        this.loading.set(false);
        this.currentStep.set(3);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set('Erreur lors de la génération avec l\'assistant IA.');
      }
    });
  }

  private injectProfileDataIntoSample(res: AiPersonaGenerationResponse): void {
    this.profileService.getProfile().subscribe({
      next: (profileRes) => {
        if (profileRes.data) {
          const pi = profileRes.data.personalInfo || {};
          if (res.sampleData) {
            if (pi.nom) res.sampleData['nom'] = pi.nom;
            if (pi.prenom) res.sampleData['prenom'] = pi.prenom;
            if (pi.prenom && pi.nom) {
              res.sampleData['nom_complet'] = `${pi.prenom} ${pi.nom}`;
              res.sampleData['full_name'] = `${pi.prenom} ${pi.nom}`;
              res.sampleData['nom_etudiant'] = `${pi.prenom} ${pi.nom}`;
            }
            if (pi.email) {
              res.sampleData['email'] = pi.email;
              res.sampleData['emetteur_email'] = pi.email;
            }
            if (pi.telephone) {
              res.sampleData['telephone'] = pi.telephone;
              res.sampleData['phone'] = pi.telephone;
            }
            if (pi.ville) {
              res.sampleData['ville_province'] = pi.ville;
              res.sampleData['city_state'] = pi.ville;
            }
            if (pi.titreProfessionnel) {
              res.sampleData['titre_professionnel'] = pi.titreProfessionnel;
              res.sampleData['recent_title'] = pi.titreProfessionnel;
            }
            if (pi.bioSommaire) {
              res.sampleData['profil_sommaire'] = pi.bioSommaire;
              res.sampleData['summary'] = pi.bioSommaire;
            }
            if (pi.linkedin) {
              res.sampleData['linkedin'] = pi.linkedin;
            }
          }
        }
      },
      error: () => {
        // Silently keep default sampleData
      }
    });
  }

  applyToDesigner(): void {
    if (this.generatedResponse) {
      this.templateGenerated.emit(this.generatedResponse);
      this.closeModal();
    }
  }

  closeModal(): void {
    this.isOpen = false;
    this.close.emit();
    this.currentStep.set(1);
    this.generatedResponse = null;
  }
}
