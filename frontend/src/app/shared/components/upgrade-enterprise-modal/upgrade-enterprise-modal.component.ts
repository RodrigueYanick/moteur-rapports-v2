import { Component, EventEmitter, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LucideAngularModule, Building, CheckCircle2, ShieldCheck, Zap, X, ArrowRight, Loader2, Users } from 'lucide-angular';
import { AuthService } from '../../../services/auth.service';
import { EnterpriseUpgradeRequest, LoginResponse } from '../../../models/auth.model';

@Component({
  selector: 'app-upgrade-enterprise-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './upgrade-enterprise-modal.component.html',
  styleUrls: ['./upgrade-enterprise-modal.component.scss']
})
export class UpgradeEnterpriseModalComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  @Output() closed = new EventEmitter<void>();
  @Output() upgraded = new EventEmitter<LoginResponse>();

  readonly icons = {
    building: Building,
    check: CheckCircle2,
    shield: ShieldCheck,
    zap: Zap,
    close: X,
    arrowRight: ArrowRight,
    loader: Loader2,
    users: Users
  };

  form: EnterpriseUpgradeRequest = {
    nomEntreprise: '',
    codeEntreprise: '',
    siret: '',
    adresse: '',
    telephone: '',
    emailContact: ''
  };

  loading = false;
  errorMessage = '';
  successMessage = '';

  submitUpgrade(): void {
    if (!this.form.nomEntreprise || this.form.nomEntreprise.trim().length < 2) {
      this.errorMessage = "Le nom de l'organisation est obligatoire (minimum 2 caractères).";
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.authService.upgradeToEnterprise(this.form).subscribe({
      next: (res) => {
        this.loading = false;
        this.successMessage = "🎉 Félicitations ! Votre espace Entreprise est maintenant activé.";
        this.upgraded.emit(res);
        setTimeout(() => {
          this.closed.emit();
          this.router.navigate(['/bibliotheque']);
        }, 1200);
      },
      error: (err) => {
        this.loading = false;
        if (err.error?.errors?.length > 0) {
          this.errorMessage = err.error.errors[0];
        } else if (err.error?.message) {
          this.errorMessage = err.error.message;
        } else {
          this.errorMessage = "Une erreur est survenue lors de la création de l'espace entreprise.";
        }
      }
    });
  }

  closeModal(): void {
    if (!this.loading) {
      this.closed.emit();
    }
  }
}

