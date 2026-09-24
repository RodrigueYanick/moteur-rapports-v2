import {
  Directive,
  ElementRef,
  Input,
  Output,
  EventEmitter,
  HostListener,
  OnInit,
  OnDestroy,
  NgZone
} from '@angular/core';
import { Variable } from '../../models/variable.model';

export type AutocompleteVariableItem = Variable | {
  nomVariable: string;
  type?: string;
  description?: string;
  obligatoire?: boolean;
};

@Directive({
  selector: '[appVariableAutocomplete]',
  standalone: true,
})
export class VariableAutocompleteDirective implements OnInit, OnDestroy {
  @Input('appVariableAutocomplete') variables: AutocompleteVariableItem[] = [];
  @Input() autocompleteEnabled: boolean = true;
  @Output() variableSelected = new EventEmitter<AutocompleteVariableItem>();

  private hostEl: HTMLTextAreaElement | HTMLInputElement;
  private dropdownEl: HTMLDivElement | null = null;
  private filteredVars: AutocompleteVariableItem[] = [];
  private selectedIndex: number = 0;
  private isOpen: boolean = false;
  private currentQuery: string = '';

  private docClickListener: ((e: MouseEvent) => void) | null = null;
  private winResizeListener: (() => void) | null = null;
  private winScrollListener: (() => void) | null = null;

  constructor(private el: ElementRef, private ngZone: NgZone) {
    this.hostEl = this.el.nativeElement;
  }

  ngOnInit(): void {
    this.injectStylesIfNeeded();

    this.docClickListener = (event: MouseEvent) => {
      if (!this.isOpen) return;
      const target = event.target as HTMLElement;
      if (this.dropdownEl && this.dropdownEl.contains(target)) return;
      if (this.hostEl.contains(target)) return;
      this.closeDropdown();
    };
    document.addEventListener('click', this.docClickListener);

    this.winResizeListener = () => {
      if (this.isOpen) this.updateDropdownPosition();
    };
    window.addEventListener('resize', this.winResizeListener);

    this.winScrollListener = () => {
      if (this.isOpen) this.updateDropdownPosition();
    };
    window.addEventListener('scroll', this.winScrollListener, true);
  }

  ngOnDestroy(): void {
    this.closeDropdown();
    if (this.docClickListener) {
      document.removeEventListener('click', this.docClickListener);
    }
    if (this.winResizeListener) {
      window.removeEventListener('resize', this.winResizeListener);
    }
    if (this.winScrollListener) {
      window.removeEventListener('scroll', this.winScrollListener, true);
    }
  }

  @HostListener('input')
  onInput(): void {
    if (!this.autocompleteEnabled) return;
    this.checkCursorAndFilter();
  }

  @HostListener('keyup', ['$event'])
  onKeyUp(event: KeyboardEvent): void {
    if (!this.autocompleteEnabled) return;
    if (['ArrowDown', 'ArrowUp', 'Enter', 'Tab', 'Escape'].includes(event.key)) {
      return;
    }
    this.checkCursorAndFilter();
  }

  @HostListener('click')
  onClick(): void {
    if (!this.autocompleteEnabled) return;
    this.checkCursorAndFilter();
  }

  @HostListener('blur')
  onBlur(): void {
    // Petit délai pour permettre au clic sur le dropdown d'être exécuté
    setTimeout(() => {
      if (this.isOpen) {
        this.closeDropdown();
      }
    }, 200);
  }

  @HostListener('keydown', ['$event'])
  onKeyDown(event: KeyboardEvent): void {
    if (!this.isOpen || this.filteredVars.length === 0) return;

    if (['ArrowDown', 'ArrowUp', 'Enter', 'Tab', 'Escape'].includes(event.key)) {
      (event as any)._autocompleteHandled = true;
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault();
      event.stopImmediatePropagation();
      this.selectedIndex = (this.selectedIndex + 1) % this.filteredVars.length;
      this.updateActiveItemClass();
      this.scrollActiveItemIntoView();
      return;
    }

    if (event.key === 'ArrowUp') {
      event.preventDefault();
      event.stopImmediatePropagation();
      this.selectedIndex = (this.selectedIndex - 1 + this.filteredVars.length) % this.filteredVars.length;
      this.updateActiveItemClass();
      this.scrollActiveItemIntoView();
      return;
    }

    if (event.key === 'Enter' || event.key === 'Tab') {
      event.preventDefault();
      event.stopImmediatePropagation();
      const selected = this.filteredVars[this.selectedIndex];
      if (selected) {
        this.insertVariable(selected);
      }
      return;
    }

    if (event.key === 'Escape') {
      event.preventDefault();
      event.stopImmediatePropagation();
      this.closeDropdown();
      return;
    }
  }

  private checkCursorAndFilter(): void {
    const val = this.hostEl.value || '';
    let cursorPos = this.hostEl.selectionStart;
    if (cursorPos == null || (cursorPos === 0 && val.length > 0 && val.includes('{{'))) {
      cursorPos = val.length;
    }
    const textBeforeCursor = val.substring(0, cursorPos);

    const braceIndex = textBeforeCursor.lastIndexOf('{{');
    if (braceIndex === -1) {
      this.closeDropdown();
      return;
    }

    const query = textBeforeCursor.substring(braceIndex + 2);
    // Si la chaîne après {{ contient une accolade fermante } ou un retour à la ligne, on n'autocomplète pas
    if (query.includes('}') || query.includes('\n')) {
      this.closeDropdown();
      return;
    }

    // Requête valide (ex: "" après {{, ou "cli")
    if (!/^[a-zA-Z0-9_]*$/.test(query)) {
      this.closeDropdown();
      return;
    }

    this.currentQuery = query;
    const qLower = query.toLowerCase();

    // Filtrer les variables
    const vars = this.variables || [];
    this.filteredVars = vars.filter(v =>
      v.nomVariable.toLowerCase().includes(qLower)
    ).sort((a, b) => {
      const aStarts = a.nomVariable.toLowerCase().startsWith(qLower);
      const bStarts = b.nomVariable.toLowerCase().startsWith(qLower);
      if (aStarts && !bStarts) return -1;
      if (!aStarts && bStarts) return 1;
      return a.nomVariable.localeCompare(b.nomVariable);
    });

    if (this.filteredVars.length > 0) {
      this.selectedIndex = 0;
      this.openDropdown();
    } else {
      this.closeDropdown();
    }
  }

  private openDropdown(): void {
    if (!this.dropdownEl) {
      this.dropdownEl = document.createElement('div');
      this.dropdownEl.className = 'variable-autocomplete-dropdown';
      document.body.appendChild(this.dropdownEl);
    }
    this.isOpen = true;
    this.renderDropdownContent();
    this.updateDropdownPosition();
  }

  private closeDropdown(): void {
    this.isOpen = false;
    if (this.dropdownEl && this.dropdownEl.parentNode) {
      this.dropdownEl.parentNode.removeChild(this.dropdownEl);
      this.dropdownEl = null;
    }
  }

  private updateDropdownPosition(): void {
    if (!this.dropdownEl) return;
    const rect = this.hostEl.getBoundingClientRect();
    const dropdownHeight = 220; // Estimation hauteur
    const spaceBelow = window.innerHeight - rect.bottom;
    const showAbove = spaceBelow < dropdownHeight && rect.top > dropdownHeight;

    const width = Math.max(280, Math.min(rect.width, 380));
    let left = rect.left;
    if (left + width > window.innerWidth - 10) {
      left = Math.max(10, window.innerWidth - width - 15);
    }

    this.dropdownEl.style.position = 'fixed';
    this.dropdownEl.style.width = `${width}px`;
    this.dropdownEl.style.left = `${left}px`;
    this.dropdownEl.style.zIndex = '999999';

    if (showAbove) {
      this.dropdownEl.style.bottom = `${window.innerHeight - rect.top + 4}px`;
      this.dropdownEl.style.top = 'auto';
    } else {
      this.dropdownEl.style.top = `${rect.bottom + 4}px`;
      this.dropdownEl.style.bottom = 'auto';
    }
  }

  private renderDropdownContent(): void {
    if (!this.dropdownEl) return;

    let html = `
      <div class="vac-header">
        <span class="vac-header-title">Variables disponibles (${this.filteredVars.length})</span>
        <span class="vac-header-hint">Insérer via {{...}}</span>
      </div>
      <div class="vac-list">
    `;

    this.filteredVars.forEach((v, index) => {
      const isSelected = index === this.selectedIndex;
      const type = (v.type || 'STRING').toUpperCase();
      const typeClass = 'type-' + type.toLowerCase();
      const highlightedName = this.highlightMatch(v.nomVariable, this.currentQuery);

      html += `
        <div class="vac-item ${isSelected ? 'selected' : ''}" data-index="${index}">
          <div class="vac-item-top">
            <span class="vac-var-name">${highlightedName}</span>
            <span class="vac-var-badge ${typeClass}">${type}</span>
          </div>
          ${v.description ? `<div class="vac-var-desc">${this.escapeHtml(v.description)}</div>` : ''}
        </div>
      `;
    });

    html += `
      </div>
      <div class="vac-footer">
        <span>↑↓ Naviguer</span> · <span>Entrée Insérer</span> · <span>Échap Fermer</span>
      </div>
    `;

    this.dropdownEl.innerHTML = html;

    // Attacher les écouteurs de clic et survol sur les éléments
    const items = this.dropdownEl.querySelectorAll('.vac-item');
    items.forEach((itemEl) => {
      const idx = parseInt(itemEl.getAttribute('data-index') || '0', 10);
      itemEl.addEventListener('mouseenter', () => {
        this.selectedIndex = idx;
        this.updateActiveItemClass();
      });
      itemEl.addEventListener('mousedown', (e) => {
        e.preventDefault(); // Empêche le blur prématuré de l'input
        e.stopPropagation();
      });
      itemEl.addEventListener('click', (e) => {
        e.preventDefault();
        e.stopPropagation();
        this.insertVariable(this.filteredVars[idx]);
      });
    });
  }

  private updateActiveItemClass(): void {
    if (!this.dropdownEl) return;
    const items = this.dropdownEl.querySelectorAll('.vac-item');
    items.forEach((itemEl, idx) => {
      if (idx === this.selectedIndex) {
        itemEl.classList.add('selected');
      } else {
        itemEl.classList.remove('selected');
      }
    });
  }

  private scrollActiveItemIntoView(): void {
    if (!this.dropdownEl) return;
    const selected = this.dropdownEl.querySelector('.vac-item.selected') as HTMLElement;
    if (selected) {
      selected.scrollIntoView({ block: 'nearest' });
    }
  }

  private insertVariable(v: AutocompleteVariableItem): void {
    const el = this.hostEl;
    const val = el.value || '';
    let pos = el.selectionStart;
    if (pos == null || (pos === 0 && val.length > 0 && val.includes('{{'))) {
      pos = val.length;
    }
    const before = val.substring(0, pos);
    const braceIndex = before.lastIndexOf('{{');

    if (braceIndex === -1) {
      this.closeDropdown();
      return;
    }

    const prefix = before.substring(0, braceIndex);
    const after = val.substring(pos);
    const insertion = `{{${v.nomVariable}}}`;
    const newVal = prefix + insertion + after;

    el.value = newVal;
    const newPos = prefix.length + insertion.length;
    el.setSelectionRange(newPos, newPos);

    // Émettre les événements pour que NgModel ou ReactiveForms se synchronise
    el.dispatchEvent(new Event('input', { bubbles: true }));
    el.dispatchEvent(new Event('change', { bubbles: true }));

    this.variableSelected.emit(v);
    this.closeDropdown();
    el.focus();
  }

  private highlightMatch(text: string, query: string): string {
    if (!query) return this.escapeHtml(text);
    const index = text.toLowerCase().indexOf(query.toLowerCase());
    if (index === -1) return this.escapeHtml(text);

    const before = text.substring(0, index);
    const match = text.substring(index, index + query.length);
    const after = text.substring(index + query.length);

    return `${this.escapeHtml(before)}<mark class="vac-mark">${this.escapeHtml(match)}</mark>${this.escapeHtml(after)}`;
  }

  private escapeHtml(str: string): string {
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  private injectStylesIfNeeded(): void {
    const styleId = 'vac-dropdown-styles';
    if (document.getElementById(styleId)) return;

    const styleEl = document.createElement('style');
    styleEl.id = styleId;
    styleEl.textContent = `
      .variable-autocomplete-dropdown {
        background: #ffffff;
        border: 1px solid #cbd5e1;
        border-radius: 8px;
        box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.15), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
        overflow: hidden;
        display: flex;
        flex-direction: column;
        animation: vacFadeIn 0.15s ease-out;
      }
      @keyframes vacFadeIn {
        from { opacity: 0; transform: translateY(-4px); }
        to { opacity: 1; transform: translateY(0); }
      }
      .vac-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        background: #f8fafc;
        border-bottom: 1px solid #e2e8f0;
        padding: 6px 10px;
        font-size: 11px;
      }
      .vac-header-title {
        font-weight: 700;
        color: #475569;
        text-transform: uppercase;
        letter-spacing: 0.04em;
      }
      .vac-header-hint {
        color: #94a3b8;
        font-size: 10px;
      }
      .vac-list {
        max-height: 200px;
        overflow-y: auto;
        padding: 4px 0;
      }
      .vac-item {
        padding: 6px 12px;
        cursor: pointer;
        display: flex;
        flex-direction: column;
        gap: 2px;
        transition: background 0.1s ease;
      }
      .vac-item:hover, .vac-item.selected {
        background: #eff6ff;
        border-left: 3px solid #2563eb;
        padding-left: 9px;
      }
      .vac-item-top {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }
      .vac-var-name {
        font-family: 'Consolas', 'Monaco', monospace;
        font-size: 12px;
        font-weight: 600;
        color: #0f172a;
      }
      .vac-mark {
        background: #fef08a;
        color: #854d0e;
        border-radius: 2px;
        padding: 0 1px;
      }
      .vac-var-badge {
        font-size: 9px;
        font-weight: 700;
        padding: 1px 5px;
        border-radius: 4px;
        text-transform: uppercase;
        letter-spacing: 0.03em;
      }
      .vac-var-badge.type-string { background: #dbeafe; color: #1d4ed8; }
      .vac-var-badge.type-float { background: #dcfce7; color: #15803d; }
      .vac-var-badge.type-date { background: #f3e8ff; color: #7e22ce; }
      .vac-var-badge.type-boolean { background: #fef3c7; color: #b45309; }
      .vac-var-badge.type-array { background: #ccfbf1; color: #0f766e; }
      .vac-var-badge.type-calculee { background: #e0e7ff; color: #4338ca; }
      .vac-var-badge.type-image { background: #ffedd5; color: #c2410c; }
      .vac-var-desc {
        font-size: 10.5px;
        color: #64748b;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
      .vac-footer {
        background: #f8fafc;
        border-top: 1px solid #f1f5f9;
        padding: 4px 10px;
        font-size: 10px;
        color: #94a3b8;
        text-align: center;
      }
      .vac-footer span {
        font-weight: 500;
        color: #64748b;
      }
    `;
    document.head.appendChild(styleEl);
  }
}
