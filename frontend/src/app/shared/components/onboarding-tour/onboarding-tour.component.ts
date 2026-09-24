import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectorRef,
  ElementRef,
  HostListener,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { OnboardingService, OnboardingStep } from '../../services/onboarding.service';
import {
  LucideAngularModule,
  Compass,
  ArrowRight,
  ArrowLeft,
  Check,
  X,
  Sparkles,
} from 'lucide-angular';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-onboarding-tour',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './onboarding-tour.component.html',
  styleUrls: ['./onboarding-tour.component.scss'],
})
export class OnboardingTourComponent implements OnInit, OnDestroy {
  active = false;
  currentStepIndex = 0;
  spotlightStyle: Record<string, string> = {};
  tooltipStyle: Record<string, string> = {};

  readonly icons = {
    compass: Compass,
    arrowRight: ArrowRight,
    arrowLeft: ArrowLeft,
    check: Check,
    close: X,
    sparkles: Sparkles,
  };

  private sub?: Subscription;

  constructor(
    public onboardingService: OnboardingService,
    private cdr: ChangeDetectorRef,
    private el: ElementRef
  ) {}

  ngOnInit(): void {
    this.sub = this.onboardingService.active$.subscribe((isActive) => {
      this.active = isActive;
      if (isActive) {
        this.updatePosition();
      }
      this.cdr.detectChanges();
    });

    this.onboardingService.currentStepIndex$.subscribe((idx) => {
      this.currentStepIndex = idx;
      if (this.active) {
        setTimeout(() => this.updatePosition(), 50);
      }
      this.cdr.detectChanges();
    });
  }

  ngOnDestroy(): void {
    if (this.sub) {
      this.sub.unsubscribe();
    }
  }

  get step(): OnboardingStep {
    return this.onboardingService.steps[this.currentStepIndex] || this.onboardingService.steps[0];
  }

  get isLastStep(): boolean {
    return this.currentStepIndex === this.onboardingService.steps.length - 1;
  }

  @HostListener('window:resize')
  onResize(): void {
    if (this.active) {
      this.updatePosition();
    }
  }

  @HostListener('window:keydown.escape')
  onEscape(): void {
    if (this.active) {
      this.onboardingService.skip();
    }
  }

  updatePosition(): void {
    const targetSel = this.step.targetSelector;
    let rect: DOMRect | null = null;

    if (targetSel) {
      const targetEl = document.querySelector(targetSel);
      if (targetEl) {
        targetEl.scrollIntoView({ block: 'nearest', inline: 'nearest' });
        rect = targetEl.getBoundingClientRect();
      }
    }

    if (rect && rect.width > 0 && rect.height > 0) {
      // Positionnement du spotlight
      const pad = 8;
      this.spotlightStyle = {
        top: `${Math.max(0, rect.top - pad)}px`,
        left: `${Math.max(0, rect.left - pad)}px`,
        width: `${rect.width + pad * 2}px`,
        height: `${rect.height + pad * 2}px`,
      };

      // Positionnement de l'infobulle
      let top = rect.top + 20;
      let left = rect.left;

      if (this.step.position === 'bottom') {
        top = rect.bottom + 16;
        left = rect.left + rect.width / 2 - 170;
      } else if (this.step.position === 'left') {
        top = rect.top + 20;
        left = rect.left - 360;
      } else if (this.step.position === 'right') {
        top = rect.top + 20;
        left = rect.right + 20;
      }

      // Toujours borner l'infobulle dans la fenêtre visible (viewport)
      const popoverWidth = 360;
      const popoverHeight = 240;
      top = Math.max(16, Math.min(top, window.innerHeight - popoverHeight));
      left = Math.max(16, Math.min(left, window.innerWidth - popoverWidth));

      this.tooltipStyle = {
        top: `${top}px`,
        left: `${left}px`,
        right: 'auto',
      };
    } else {
      // Fallback centré à l'écran
      this.spotlightStyle = { display: 'none' };
      this.tooltipStyle = {
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
      };
    }

    this.cdr.detectChanges();
  }
}

