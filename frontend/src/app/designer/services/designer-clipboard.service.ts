import { Injectable, signal } from '@angular/core';
import { DesignBlock } from '../models/design-block.model';

@Injectable({
  providedIn: 'root',
})
export class DesignerClipboardService {
  private readonly _copiedBlock = signal<DesignBlock | null>(null);
  readonly copiedBlock = this._copiedBlock.asReadonly();

  canCopy(isLocked: boolean, block: DesignBlock | null): boolean {
    return !isLocked && !!block;
  }

  canPaste(isLocked: boolean): boolean {
    return !isLocked && !!this._copiedBlock();
  }

  copy(block: DesignBlock): void {
    if (!block) return;
    this._copiedBlock.set(JSON.parse(JSON.stringify(block)));
  }

  paste(clampCallback: (b: DesignBlock) => void): DesignBlock | null {
    const source = this._copiedBlock();
    if (!source) return null;

    const cloned: DesignBlock = JSON.parse(JSON.stringify(source));
    cloned.id = crypto.randomUUID();
    cloned.x = (cloned.x || 0) + 20;
    cloned.y = (cloned.y || 0) + 20;

    clampCallback(cloned);
    return cloned;
  }

  duplicate(block: DesignBlock, clampCallback: (b: DesignBlock) => void): DesignBlock {
    const cloned: DesignBlock = JSON.parse(JSON.stringify(block));
    cloned.id = crypto.randomUUID();
    cloned.x = (cloned.x || 0) + 20;
    cloned.y = (cloned.y || 0) + 20;

    clampCallback(cloned);
    return cloned;
  }

  clear(): void {
    this._copiedBlock.set(null);
  }
}

