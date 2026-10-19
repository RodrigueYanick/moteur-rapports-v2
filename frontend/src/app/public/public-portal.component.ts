import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import {
  LucideAngularModule,
  GraduationCap,
  Briefcase,
  Receipt,
  Sparkles,
  ArrowRight,
  Eye,
  CheckCircle2,
  Download,
  ShieldCheck,
  Layers,
  Zap,
  Users,
  Search,
  FileText,
  ExternalLink,
  Building2,
  X,
  FileCheck
} from 'lucide-angular';
import { PublicPortalService, PublicCategory, PublicStats } from '../services/public-portal.service';
import { TemplateResponse } from '../models/template.model';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-public-portal',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, LucideAngularModule],
  templateUrl: './public-portal.component.html',
  styleUrls: ['./public-portal.component.scss']
})
export class PublicPortalComponent implements OnInit {
  private portalService = inject(PublicPortalService);
  private authService = inject(AuthService);
  private router = inject(Router);
  private sanitizer = inject(DomSanitizer);

  readonly icons = {
    graduation: GraduationCap,
    briefcase: Briefcase,
    receipt: Receipt,
    sparkles: Sparkles,
    arrowRight: ArrowRight,
    eye: Eye,
    check: CheckCircle2,
    download: Download,
    shield: ShieldCheck,
    layers: Layers,
    zap: Zap,
    users: Users,
    search: Search,
    file: FileText,
    external: ExternalLink,
    building: Building2,
    close: X,
    fileCheck: FileCheck
  };

  templates: TemplateResponse[] = [];
  categories: PublicCategory[] = [];
  publicStats: PublicStats | null = null;
  animatedDocsCount = 0;
  animatedUsersCount = 0;
  animatedTemplatesCount = 0;
  loading = true;
  searchTerm = '';
  selectedPersona: 'ALL' | 'STUDENT' | 'CANDIDATE' | 'FREELANCE' | 'INDIVIDUAL' = 'ALL';
  selectedCategoryCode = '';

  // Modal d'aperçu
  previewModalOpen = false;
  previewTemplate: TemplateResponse | null = null;
  previewPdfUrl: string | null = null;
  safePdfUrl: SafeResourceUrl | null = null;
  generatingPreview = false;

  get isLoggedIn(): boolean {
    return this.authService.isAuthenticated();
  }

  get currentUser() {
    return this.authService.getCurrentUser();
  }

  ngOnInit(): void {
    this.loadStats();
    this.loadCategories();
    this.loadTemplates();
  }

  loadStats(): void {
    this.portalService.getPublicStats().subscribe({
      next: (stats) => {
        this.publicStats = stats;
        this.animateCounters(stats);
      },
      error: () => {
        this.animateCounters({
          totalDocuments: 250150,
          totalUsers: 45200,
          totalTemplates: 125,
          satisfactionRate: 99.4,
          activeCountries: 38,
          availableCategories: 8
        });
      }
    });
  }

  animateCounters(stats: PublicStats): void {
    const duration = 1200;
    const startTime = performance.now();
    const startDocs = 0;
    const startUsers = 0;
    const startTemplates = 0;

    const step = (currentTime: number) => {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      const ease = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress);

      this.animatedDocsCount = Math.floor(startDocs + (stats.totalDocuments - startDocs) * ease);
      this.animatedUsersCount = Math.floor(startUsers + (stats.totalUsers - startUsers) * ease);
      this.animatedTemplatesCount = Math.floor(startTemplates + (stats.totalTemplates - startTemplates) * ease);

      if (progress < 1) {
        requestAnimationFrame(step);
      }
    };

    requestAnimationFrame(step);
  }

  loadCategories(): void {
    this.portalService.getPublicCategories().subscribe({
      next: (data) => {
        this.categories = data;
      },
      error: (err) => console.error('Erreur chargement catégories', err)
    });
  }

  loadTemplates(): void {
    this.loading = true;
    const cat = this.selectedCategoryCode ? this.selectedCategoryCode : undefined;
    const q = this.searchTerm.trim() ? this.searchTerm.trim() : undefined;

    this.portalService.getPublicTemplates(cat, q).subscribe({
      next: (data) => {
        this.templates = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur chargement modèles publics', err);
        this.loading = false;
      }
    });
  }

  filterByPersona(persona: 'ALL' | 'STUDENT' | 'CANDIDATE' | 'FREELANCE' | 'INDIVIDUAL'): void {
    this.selectedPersona = persona;
    this.selectedCategoryCode = '';
    this.loadTemplates();
  }

  filterByCategory(code: string): void {
    if (this.selectedCategoryCode === code) {
      this.selectedCategoryCode = '';
    } else {
      this.selectedCategoryCode = code;
    }
    this.loadTemplates();
  }

  get filteredTemplates(): TemplateResponse[] {
    if (this.selectedPersona === 'ALL') {
      return this.templates;
    }
    const matchingCatCodes = this.categories
      .filter((c) => c.persona === this.selectedPersona)
      .map((c) => c.code);

    return this.templates.filter((t) => t.categorie && matchingCatCodes.includes(t.categorie));
  }

  openPreview(t: TemplateResponse): void {
    this.previewTemplate = t;
    this.previewModalOpen = true;
    this.generatingPreview = true;
    this.previewPdfUrl = null;
    this.safePdfUrl = null;

    this.portalService.demoRender(t.id).subscribe({
      next: (blob) => {
        const fileUrl = URL.createObjectURL(blob);
        this.previewPdfUrl = fileUrl;
        this.safePdfUrl = this.sanitizer.bypassSecurityTrustResourceUrl(fileUrl);
        this.generatingPreview = false;
      },
      error: (err) => {
        console.error('Erreur de rendu démo', err);
        this.generatingPreview = false;
      }
    });
  }

  closePreview(): void {
    if (this.previewPdfUrl) {
      URL.revokeObjectURL(this.previewPdfUrl);
      this.previewPdfUrl = null;
      this.safePdfUrl = null;
    }
    this.previewModalOpen = false;
    this.previewTemplate = null;
  }

  customizeTemplate(t: TemplateResponse): void {
    if (this.isLoggedIn) {
      this.router.navigate(['/templates', t.id]);
    } else {
      // Redirection vers l'inscription avec retour automatique
      this.router.navigate(['/auth/register'], {
        queryParams: { templateId: t.id, accountType: 'PERSONAL' }
      });
    }
  }

  goToEnterprise(): void {
    if (this.isLoggedIn) {
      this.router.navigate(['/bibliotheque']);
    } else {
      this.router.navigate(['/auth/register'], {
        queryParams: { accountType: 'ENTERPRISE' }
      });
    }
  }
}
