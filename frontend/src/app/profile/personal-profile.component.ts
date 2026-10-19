import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ProfileService } from '../services/profile.service';
import { AuthService } from '../services/auth.service';
import { PersonaProfileDto, ExperienceItem, FormationItem } from '../models/profile.model';
import { UpgradeEnterpriseModalComponent } from '../shared/components/upgrade-enterprise-modal/upgrade-enterprise-modal.component';

@Component({
  selector: 'app-personal-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, UpgradeEnterpriseModalComponent],
  templateUrl: './personal-profile.component.html',
  styleUrls: ['./personal-profile.component.scss']
})
export class PersonalProfileComponent implements OnInit {
  showUpgradeModal = false;
  activeTab = signal<'identity' | 'experiences' | 'formations' | 'skills'>('identity');
  loading = signal<boolean>(false);
  saving = signal<boolean>(false);
  successMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);

  profile: PersonaProfileDto = {
    personalInfo: {},
    experiences: [],
    formations: [],
    competences: {
      techniques: [],
      outils: [],
      langues: [],
      certifications: []
    },
    preferences: {}
  };

  newCompetenceTech = '';
  newCompetenceOutil = '';
  newLangue = '';

  constructor(
    private profileService: ProfileService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading.set(true);
    this.profileService.getProfile().subscribe({
      next: (res) => {
        if (res.data) {
          this.profile = {
            personalInfo: res.data.personalInfo || {},
            experiences: res.data.experiences || [],
            formations: res.data.formations || [],
            competences: {
              techniques: res.data.competences?.techniques || [],
              outils: res.data.competences?.outils || [],
              langues: res.data.competences?.langues || [],
              certifications: res.data.competences?.certifications || []
            },
            preferences: res.data.preferences || {}
          };
        }
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set('Erreur lors du chargement de votre profil.');
        this.loading.set(false);
      }
    });
  }

  saveProfile(): void {
    this.saving.set(true);
    this.successMessage.set(null);
    this.errorMessage.set(null);

    this.profileService.updateProfile(this.profile).subscribe({
      next: () => {
        this.saving.set(false);
        this.successMessage.set('Votre profil a été enregistré avec succès ! Vos documents peuvent être pré-remplis en un clic.');
        setTimeout(() => this.successMessage.set(null), 5000);
      },
      error: () => {
        this.saving.set(false);
        this.errorMessage.set('Erreur lors de la sauvegarde du profil.');
      }
    });
  }

  // --- Gestion des Expériences ---
  addExperience(): void {
    this.profile.experiences.push({
      titre: '',
      entreprise: '',
      ville: '',
      dateDebut: '',
      dateFin: '',
      enCours: false,
      description: ''
    });
  }

  removeExperience(index: number): void {
    this.profile.experiences.splice(index, 1);
  }

  // --- Gestion des Formations ---
  addFormation(): void {
    this.profile.formations.push({
      diplome: '',
      etablissement: '',
      ville: '',
      anneeObtention: '',
      details: ''
    });
  }

  removeFormation(index: number): void {
    this.profile.formations.splice(index, 1);
  }

  // --- Gestion des Compétences ---
  addSkill(type: 'techniques' | 'outils' | 'langues'): void {
    let val = '';
    if (type === 'techniques') {
      val = this.newCompetenceTech.trim();
      this.newCompetenceTech = '';
    } else if (type === 'outils') {
      val = this.newCompetenceOutil.trim();
      this.newCompetenceOutil = '';
    } else if (type === 'langues') {
      val = this.newLangue.trim();
      this.newLangue = '';
    }

    if (val && !this.profile.competences[type]?.includes(val)) {
      if (!this.profile.competences[type]) {
        this.profile.competences[type] = [];
      }
      this.profile.competences[type].push(val);
    }
  }

  removeSkill(type: 'techniques' | 'outils' | 'langues', index: number): void {
    this.profile.competences[type]?.splice(index, 1);
  }
}

