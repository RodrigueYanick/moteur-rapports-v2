import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, ToastItem } from '../../services/toast.service';
import {
  LucideAngularModule,
  CheckCircle,
  AlertCircle,
  Info,
  AlertTriangle,
  X,
} from 'lucide-angular';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  templateUrl: './toast-container.component.html',
  styleUrls: ['./toast-container.component.scss'],
})
export class ToastContainerComponent {
  readonly icons = {
    check: CheckCircle,
    error: AlertCircle,
    info: Info,
    warning: AlertTriangle,
    close: X,
  };

  constructor(public toastService: ToastService) {}

  trackByToastId(_index: number, toast: ToastItem): string {
    return toast.id;
  }
}

