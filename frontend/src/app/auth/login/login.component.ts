import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { LucideAngularModule, Lock, Mail, ArrowRight, Sparkles, Building, UserCheck } from 'lucide-angular';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, LucideAngularModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  email = '';
  motDePasse = '';
  loading = false;
  errorMessage = '';
  returnUrl = '/bibliotheque';

  readonly icons = {
    lock: Lock,
    mail: Mail,
    arrowRight: ArrowRight,
    sparkles: Sparkles,
    building: Building,
    userCheck: UserCheck
  };

  constructor(
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/bibliotheque';
  }

  onSubmit(): void {
    if (!this.email || !this.motDePasse) {
      this.errorMessage = 'Veuillez renseigner votre email et mot de passe.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.auth.login({ email: this.email, motDePasse: this.motDePasse }).subscribe({
      next: () => {
        this.loading = false;
        this.cdr.detectChanges();
        this.router.navigateByUrl(this.returnUrl);
      },
      error: (err) => {
        this.loading = false;
        if (err.error && err.error.errors && err.error.errors.length > 0) {
          this.errorMessage = err.error.errors[0];
        } else if (err.error && err.error.message) {
          this.errorMessage = err.error.message;
        } else {
          this.errorMessage = 'Identifiants invalides ou service indisponible.';
        }
        this.cdr.detectChanges();
      }
    });
  }

  quickLogin(type: 'admin' | 'designer'): void {
    if (type === 'admin') {
      this.email = 'admin@rapports.com';
      this.motDePasse = 'admin123';
    } else {
      this.email = 'designer@rapports.com';
      this.motDePasse = 'designer123';
    }
    this.onSubmit();
  }
}

