import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule, Copy, X, Plus } from 'lucide-angular';
import { DesignPage } from '../../models/design-block.model';

@Component({
  selector: 'app-designer-page-tabs',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  templateUrl: './designer-page-tabs.component.html',
  styleUrl: './designer-page-tabs.component.scss'
})
export class DesignerPageTabsComponent {
  @Input() pages: DesignPage[] = [];
  @Input() activePageIndex = 0;
  @Input() isAutoPagination = false;
  @Input() isLocked = false;

  @Output() pageSelect = new EventEmitter<number>();
  @Output() pageAdd = new EventEmitter<void>();
  @Output() pageRemove = new EventEmitter<{ index: number; event: Event }>();
  @Output() pageDuplicate = new EventEmitter<{ index: number; event: Event }>();
  @Output() pageRename = new EventEmitter<void>();
  @Output() lockedAction = new EventEmitter<void>();

  editingPageIndex: number | null = null;

  readonly icons = {
    copy: Copy,
    close: X,
    plus: Plus
  };

  onSelect(index: number): void {
    this.pageSelect.emit(index);
  }

  onAdd(): void {
    if (this.isLocked || this.isAutoPagination) {
      this.lockedAction.emit();
      return;
    }
    this.pageAdd.emit();
  }

  onRemove(index: number, event: Event): void {
    event.stopPropagation();
    if (this.isLocked || this.isAutoPagination) {
      this.lockedAction.emit();
      return;
    }
    this.pageRemove.emit({ index, event });
  }

  onDuplicate(index: number, event: Event): void {
    event.stopPropagation();
    if (this.isLocked) {
      this.lockedAction.emit();
      return;
    }
    this.pageDuplicate.emit({ index, event });
  }

  startRename(index: number, event: Event): void {
    event.stopPropagation();
    if (this.isLocked) {
      this.lockedAction.emit();
      return;
    }
    this.editingPageIndex = index;
  }

  finishRename(): void {
    this.editingPageIndex = null;
    this.pageRename.emit();
  }
}

