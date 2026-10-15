import { Component, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import {
  LucideAngularModule,
  UserPlus,
  Mail,
  Lock,
  Building,
  User,
  ArrowRight,
  GraduationCap,
  Briefcase,
  Receipt,
  Sparkles,
  Check
} from 'lucide-angular';
import { AccountType, TargetPersona } from '../../models/user.model';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, LucideAngularModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent implements OnInit {
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private cdr = inject(ChangeDetectorRef);

  accountType: AccountType = 'PERSONAL';
  targetPersona: TargetPersona = 'STUDENT';

  email = '';
  motDePasse = '';
  nom = '';
  prenom = '';
  nomEntreprise = '';
  codeEntreprise = '';
  loading = false;
  errorMessage = '';
  templateIdToOpen: string | null = null;

  readonly icons = {
    userPlus: UserPlus,
    mail: Mail,
    lock: Lock,
    building: Building,
    user: User,
    arrowRight: ArrowRight,
    graduation: GraduationCap,
    briefcase: Briefcase,
    receipt: Receipt,
    sparkles: Sparkles,
    check: Check
  };

  ngOnInit(): void {
    const qAccountType = this.route.snapshot.queryParams['accountType'];
    if (qAccountType === 'ENTERPRISE') {
      this.accountType = 'ENTERPRISE';
    } else {
      this.accountType = 'PERSONAL';
    }

    this.templateIdToOpen = this.route.snapshot.queryParams['templateId'] || null;
  }

  setAccountType(type: AccountType): void {
    this.accountType = type;
    this.errorMessage = '';
  }

  setPersona(persona: TargetPersona): void {
    this.targetPersona = persona;
  }

  onSubmit(): void {
    if (!this.email || !this.motDePasse || !this.nom) {
      this.errorMessage = 'Veuillez renseigner votre nom, email et mot de passe.';
      return;
    }

    if (this.accountType === 'ENTERPRISE' && (!this.nomEntreprise || !this.nomEntreprise.trim())) {
      this.errorMessage = 'Veuillez renseigner le nom de votre entreprise pour un compte Entreprise.';
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
      accountType: this.accountType,
      targetPersona: this.accountType === 'PERSONAL' ? this.targetPersona : undefined,
      nomEntreprise: this.accountType === 'ENTERPRISE' ? this.nomEntreprise : undefined,
      codeEntreprise: this.accountType === 'ENTERPRISE' ? this.codeEntreprise : undefined
    }).subscribe({
      next: () => {
        this.loading = false;
        this.cdr.detectChanges();

        if (this.templateIdToOpen) {
          this.router.navigate(['/templates', this.templateIdToOpen]);
        } else {
          this.router.navigate(['/bibliotheque']);
        }
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
