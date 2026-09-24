import {
  Component,
  Input,
  Output,
  EventEmitter,
  HostListener,
  ViewChild,
  ElementRef,
  OnInit,
  OnChanges,
  SimpleChanges,
  ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  LucideAngularModule,
  Search,
  Plus,
  Compass,
  FileText,
  Layers,
  Sliders,
  Moon,
  Sun,
  ArrowRight,
  Sparkles,
  Check,
  FolderOpen
} from 'lucide-angular';
import { ThemeService } from '../../services/theme.service';
import { OnboardingService } from '../../services/onboarding.service';
import { TemplateApiService } from '../../../services/template-api';
import { Template } from '../../../models/template.model';

export interface CommandItem {
  id: string;
  category: 'actions' | 'navigation' | 'templates';
  label: string;
  sublabel?: string;
  badge?: string;
  icon: any;
  action: () => void;
}

@Component({
  selector: 'app-command-palette',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './command-palette.component.html',
  styleUrls: ['./command-palette.component.scss'],
})
export class CommandPaletteComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Output() isOpenChange = new EventEmitter<boolean>();

  @ViewChild('searchInput') searchInput?: ElementRef<HTMLInputElement>;

  searchQuery = '';
  selectedIndex = 0;
  templates: Template[] = [];
  isLoadingTemplates = false;

  readonly icons = {
    search: Search,
    plus: Plus,
    compass: Compass,
    fileText: FileText,
    layers: Layers,
    sliders: Sliders,
    moon: Moon,
    sun: Sun,
    arrowRight: ArrowRight,
    sparkles: Sparkles,
    check: Check,
    folderOpen: FolderOpen
  };

  constructor(
    public themeService: ThemeService,
    private onboardingService: OnboardingService,
    private templateApi: TemplateApiService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadTemplates();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.searchQuery = '';
      this.selectedIndex = 0;
      this.loadTemplates();
      setTimeout(() => {
        this.searchInput?.nativeElement?.focus();
      }, 50);
    }
  }

  @HostListener('window:keydown', ['$event'])
  handleGlobalKeydown(event: KeyboardEvent): void {
    const isMac = navigator.platform.toUpperCase().indexOf('MAC') >= 0;
    const isCommandK = (isMac ? event.metaKey : event.ctrlKey) && event.key.toLowerCase() === 'k';

    if (isCommandK) {
      event.preventDefault();
      this.toggle();
      return;
    }

    if (!this.isOpen) return;

    if (event.key === 'Escape') {
      event.preventDefault();
      this.close();
    } else if (event.key === 'ArrowDown') {
      event.preventDefault();
      const items = this.filteredItems;
      if (items.length > 0) {
        this.selectedIndex = (this.selectedIndex + 1) % items.length;
      }
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      const items = this.filteredItems;
      if (items.length > 0) {
        this.selectedIndex = (this.selectedIndex - 1 + items.length) % items.length;
      }
    } else if (event.key === 'Enter') {
      event.preventDefault();
      const items = this.filteredItems;
      if (items.length > 0 && items[this.selectedIndex]) {
        this.executeItem(items[this.selectedIndex]);
      }
    }
  }

  toggle(): void {
    if (this.isOpen) {
      this.close();
    } else {
      this.open();
    }
  }

  open(): void {
    this.isOpen = true;
    this.isOpenChange.emit(true);
    this.searchQuery = '';
    this.selectedIndex = 0;
    this.cdr.detectChanges();
    setTimeout(() => {
      this.searchInput?.nativeElement?.focus();
    }, 50);
  }

  close(): void {
    this.isOpen = false;
    this.isOpenChange.emit(false);
    this.cdr.detectChanges();
  }

  private loadTemplates(): void {
    this.isLoadingTemplates = true;
    this.templateApi.getTemplates('ALL').subscribe({
      next: (data) => {
        this.templates = data || [];
        this.isLoadingTemplates = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoadingTemplates = false;
        this.cdr.detectChanges();
      }
    });
  }

  get baseCommands(): CommandItem[] {
    return [
      {
        id: 'new-template',
        category: 'actions',
        label: 'Créer un nouveau modèle',
        sublabel: 'Ouvrir l’assistant de création ou choisir un modèle de départ',
        badge: 'Action',
        icon: this.icons.plus,
        action: () => this.router.navigate(['/templates/new'])
      },
      {
        id: 'toggle-theme',
        category: 'actions',
        label: this.themeService.isDark ? 'Passer en mode Clair' : 'Passer en mode Sombre',
        sublabel: 'Changer le thème visuel de l’application',
        badge: 'Thème',
        icon: this.themeService.isDark ? this.icons.sun : this.icons.moon,
        action: () => this.themeService.toggleTheme()
      },
      {
        id: 'start-tour',
        category: 'actions',
        label: 'Visite guidée interactive',
        sublabel: 'Relancer le guide pas-à-pas de l’atelier de conception',
        badge: 'Aide',
        icon: this.icons.compass,
        action: () => this.onboardingService.startTour(true)
      },
      {
        id: 'nav-library',
        category: 'navigation',
        label: 'Bibliothèque de modèles',
        sublabel: 'Consulter tous les modèles de rapports disponibles',
        badge: 'Nav',
        icon: this.icons.folderOpen,
        action: () => this.router.navigate(['/bibliotheque'])
      },
      {
        id: 'nav-documents',
        category: 'navigation',
        label: 'Mes documents générés',
        sublabel: 'Accéder aux PDF et exports d’archives générés',
        badge: 'Nav',
        icon: this.icons.fileText,
        action: () => this.router.navigate(['/documents'])
      },
      {
        id: 'nav-batches',
        category: 'navigation',
        label: 'Traitements par lots & Webhooks',
        sublabel: 'Générer des documents en masse et intégrations',
        badge: 'Nav',
        icon: this.icons.layers,
        action: () => this.router.navigate(['/batches'])
      },
      {
        id: 'nav-worksheet',
        category: 'navigation',
        label: 'Personnaliser la feuille de travail',
        sublabel: 'Gérer les marges, le format papier et l’orientation',
        badge: 'Nav',
        icon: this.icons.sliders,
        action: () => this.router.navigate(['/feuille-travail'])
      }
    ];
  }

  get templateCommands(): CommandItem[] {
    return this.templates.map(tpl => ({
      id: `tpl-${tpl.id}`,
      category: 'templates' as const,
      label: tpl.nom,
      sublabel: `${tpl.categorie || 'Modèle'} · v${tpl.version || '1'} · ${tpl.formatPapier || 'A4'}`,
      badge: tpl.statut,
      icon: this.icons.fileText,
      action: () => this.router.navigate(['/templates', tpl.id])
    }));
  }

  get allCommands(): CommandItem[] {
    return [...this.baseCommands, ...this.templateCommands];
  }

  get filteredItems(): CommandItem[] {
    const q = this.searchQuery.trim().toLowerCase();
    if (!q) {
      return this.allCommands.slice(0, 10);
    }
    return this.allCommands.filter(item =>
      item.label.toLowerCase().includes(q) ||
      (item.sublabel && item.sublabel.toLowerCase().includes(q)) ||
      (item.badge && item.badge.toLowerCase().includes(q))
    ).slice(0, 12);
  }

  executeItem(item: CommandItem): void {
    this.close();
    item.action();
  }

  selectItem(index: number): void {
    this.selectedIndex = index;
  }
}

