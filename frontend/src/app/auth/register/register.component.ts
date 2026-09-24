import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { LucideAngularModule, UserPlus, Mail, Lock, Building, User, ArrowRight } from 'lucide-angular';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, LucideAngularModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  email = '';
  motDePasse = '';
  nom = '';
  prenom = '';
  nomEntreprise = '';
  codeEntreprise = '';
  loading = false;
  errorMessage = '';

  readonly icons = {
    userPlus: UserPlus,
    mail: Mail,
    lock: Lock,
    building: Building,
    user: User,
    arrowRight: ArrowRight
  };

  constructor(
    private auth: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  onSubmit(): void {
    if (!this.email || !this.motDePasse || !this.nom) {
      this.errorMessage = 'Veuillez renseigner votre nom, email et mot de passe.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    this.auth.register({
      email: this.email,
      motDePasse: this.motDePasse,
      nom: this.nom,
      prenom: this.prenom,
      nomEntreprise: this.nomEntreprise,
      codeEntreprise: this.codeEntreprise
    }).subscribe({
      next: () => {
        this.loading = false;
        this.cdr.detectChanges();
        this.router.navigate(['/bibliotheque']);
      },
      error: (err) => {
        this.loading = false;
        if (err.error && err.error.errors && err.error.errors.length > 0) {
          this.errorMessage = err.error.errors[0];
        } else if (err.error && err.error.message) {
          this.errorMessage = err.error.message;
        } else {
          this.errorMessage = 'Erreur lors de l\'inscription.';
        }
        this.cdr.detectChanges();
      }
    });
  }
}

