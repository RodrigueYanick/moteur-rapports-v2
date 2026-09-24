import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface ToastItem {
  id: string;
  type: ToastType;
  message: string;
  title?: string;
  duration: number; // en ms (0 = persistant jusqu'au clic)
  createdAt: number;
}

@Injectable({
  providedIn: 'root',
})
export class ToastService {
  private toastsSubject = new BehaviorSubject<ToastItem[]>([]);
  public toasts$: Observable<ToastItem[]> = this.toastsSubject.asObservable();

  private counter = 0;

  show(type: ToastType, message: string, title?: string, duration: number = 4000): string {
    const id = `toast-${Date.now()}-${++this.counter}`;
    const newToast: ToastItem = {
      id,
      type,
      message,
      title,
      duration,
      createdAt: Date.now(),
    };

    const current = this.toastsSubject.value;
    // Limite max à 5 toasts simultanés
    const updated = [...current.slice(-4), newToast];
    this.toastsSubject.next(updated);

    if (duration > 0) {
      setTimeout(() => {
        this.remove(id);
      }, duration);
    }

    return id;
  }

  success(message: string, title?: string, duration: number = 4000): string {
    return this.show('success', message, title || 'Succès', duration);
  }

  error(message: string, title?: string, duration: number = 6000): string {
    return this.show('error', message, title || 'Erreur', duration);
  }

  info(message: string, title?: string, duration: number = 4000): string {
    return this.show('info', message, title || 'Information', duration);
  }

  warning(message: string, title?: string, duration: number = 5000): string {
    return this.show('warning', message, title || 'Attention', duration);
  }

  remove(id: string): void {
    const current = this.toastsSubject.value;
    this.toastsSubject.next(current.filter((t) => t.id !== id));
  }

  clear(): void {
    this.toastsSubject.next([]);
  }
}

