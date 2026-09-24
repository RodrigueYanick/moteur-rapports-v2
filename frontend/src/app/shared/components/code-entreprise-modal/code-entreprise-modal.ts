import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-code-entreprise-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './code-entreprise-modal.html',
  styleUrls: ['./code-entreprise-modal.scss']
})
export class CodeEntrepriseModal {
  code: string = '';
  @Output() submitted = new EventEmitter<string>();
  error: string = '';

  onSubmit(): void {
    const trimmed = this.code.trim();
    if (!trimmed) {
      this.error = 'Veuillez entrer un code entreprise.';
      return;
    }
    this.submitted.emit(trimmed);
  }
}

