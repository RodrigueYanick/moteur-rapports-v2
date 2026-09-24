import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface OnboardingStep {
  index: number;
  title: string;
  description: string;
  targetSelector?: string;
  position: 'bottom' | 'top' | 'left' | 'right' | 'center';
}

@Injectable({
  providedIn: 'root',
})
export class OnboardingService {
  private static readonly STORAGE_KEY = 'onboarding_tour_completed';

  readonly steps: OnboardingStep[] = [
    {
      index: 1,
      title: '1. Glissez vos blocs ici',
      description:
        'Concevez votre rapport en toute liberté : ajoutez des blocs de texte, logos, tableaux dynamiques ou QR codes depuis le volet latéral.',
      targetSelector: '.left-panel',
      position: 'right',
    },
    {
      index: 2,
      title: '2. Liez vos données',
      description:
        'Insérez des variables dynamiques en tapant {{ ou configurez les propriétés et le style du bloc sélectionné dans le volet droit.',
      targetSelector: '.right-panel',
      position: 'left',
    },
    {
      index: 3,
      title: '3. Générez votre PDF',
      description:
        'Basculez en mode test, prévisualisez le rendu en direct et exportez instantanément vos PDF ou vos classeurs Excel.',
      targetSelector: '.toolbar-right',
      position: 'bottom',
    },
  ];

  private activeSubject = new BehaviorSubject<boolean>(false);
  public active$: Observable<boolean> = this.activeSubject.asObservable();

  private currentStepIndexSubject = new BehaviorSubject<number>(0);
  public currentStepIndex$: Observable<number> = this.currentStepIndexSubject.asObservable();

  constructor() {}

  get isCompleted(): boolean {
    return localStorage.getItem(OnboardingService.STORAGE_KEY) === 'true';
  }

  get currentStep(): OnboardingStep {
    const idx = this.currentStepIndexSubject.value;
    return this.steps[idx] || this.steps[0];
  }

  get totalSteps(): number {
    return this.steps.length;
  }

  startTour(force: boolean = false): void {
    if (!force && this.isCompleted) return;
    this.currentStepIndexSubject.next(0);
    this.activeSubject.next(true);
  }

  next(): void {
    const current = this.currentStepIndexSubject.value;
    if (current < this.steps.length - 1) {
      this.currentStepIndexSubject.next(current + 1);
    } else {
      this.complete();
    }
  }

  prev(): void {
    const current = this.currentStepIndexSubject.value;
    if (current > 0) {
      this.currentStepIndexSubject.next(current - 1);
    }
  }

  skip(): void {
    this.complete();
  }

  complete(): void {
    localStorage.setItem(OnboardingService.STORAGE_KEY, 'true');
    this.activeSubject.next(false);
  }

  reset(): void {
    localStorage.removeItem(OnboardingService.STORAGE_KEY);
    this.startTour(true);
  }
}
