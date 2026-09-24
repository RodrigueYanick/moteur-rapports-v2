import { Injectable } from '@angular/core';
import { DesignPage } from '../models/design-block.model';

@Injectable({
  providedIn: 'root',
})
export class DesignerHistoryService {
  private undoStack: DesignPage[][] = [];
  private redoStack: DesignPage[][] = [];
  private readonly maxHistorySize = 50;

  /**
   * Clone profondément un tableau de pages pour garantir l'immutabilité de l'historique.
   */
  private clonePages(pages: DesignPage[]): DesignPage[] {
    if (!pages) return [];
    return JSON.parse(JSON.stringify(pages));
  }

  /**
   * Enregistre un nouvel état dans la pile d'annulation et vide la pile de rétablissement.
   */
  pushState(pages: DesignPage[]): void {
    if (!pages || pages.length === 0) return;
    this.undoStack.push(this.clonePages(pages));
    if (this.undoStack.length > this.maxHistorySize) {
      this.undoStack.shift();
    }
    this.redoStack = [];
  }

  /**
   * Annule la dernière action et retourne l'état précédent.
   */
  undo(currentPages: DesignPage[]): DesignPage[] | null {
    if (this.undoStack.length <= 1) return null;

    const currentState = this.undoStack.pop()!;
    this.redoStack.push(currentState);

    const previousState = this.undoStack[this.undoStack.length - 1];
    return this.clonePages(previousState);
  }

  /**
   * Rétablit la dernière action annulée et retourne l'état rétabli.
   */
  redo(currentPages: DesignPage[]): DesignPage[] | null {
    if (this.redoStack.length === 0) return null;

    const nextState = this.redoStack.pop()!;
    this.undoStack.push(nextState);

    return this.clonePages(nextState);
  }

  get canUndo(): boolean {
    return this.undoStack.length > 1;
  }

  get canRedo(): boolean {
    return this.redoStack.length > 0;
  }

  /**
   * Réinitialise complètement les piles d'historique.
   */
  clear(): void {
    this.undoStack = [];
    this.redoStack = [];
  }

  /**
   * Initialise l'historique avec l'état initial.
   */
  initialize(pages: DesignPage[]): void {
    this.clear();
    this.pushState(pages);
  }
}

