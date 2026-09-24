import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class DesignerStore {
  readonly zoomPercent = signal<number>(100);
  readonly showGrid = signal<boolean>(false);
  readonly snapEnabled = signal<boolean>(false);
  readonly showGuides = signal<boolean>(false);
  readonly fillingMode = signal<boolean>(false);
  readonly lockedMessage = signal<string | null>(null);

  private lockedMessageTimer: any;

  setZoom(zoom: number): void {
    const clamped = Math.max(25, Math.min(200, zoom));
    this.zoomPercent.set(clamped);
  }

  zoomIn(): void {
    this.setZoom(this.zoomPercent() + 10);
  }

  zoomOut(): void {
    this.setZoom(this.zoomPercent() - 10);
  }

  resetZoom(): void {
    this.setZoom(100);
  }

  toggleGrid(): void {
    this.showGrid.update(v => !v);
  }

  toggleSnap(): void {
    this.snapEnabled.update(v => !v);
  }

  toggleGuides(): void {
    this.showGuides.update(v => !v);
  }

  toggleFillingMode(): void {
    this.fillingMode.update(v => !v);
  }

  showLockedMessage(message = 'Ce modèle est publié et ne peut plus être modifié.'): void {
    this.lockedMessage.set(message);
    clearTimeout(this.lockedMessageTimer);
    this.lockedMessageTimer = setTimeout(() => {
      this.lockedMessage.set(null);
    }, 4000);
  }
}

