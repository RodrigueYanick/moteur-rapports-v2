import { Component } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TemplateApiService } from '../../services/template-api';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-template-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './template-create.html',
  styleUrls: ['./template-create.scss'],
})
export class TemplateCreate {
  error: string | null = null;
  submitting = false;
  form: FormGroup;

  constructor(private fb: FormBuilder, private api: TemplateApiService, private router: Router){
    this.form = this.fb.group({
      nom: ['', Validators.required],
      description: [''],
      contenuDesign: ['']   // <-- ajout ici
    });
  }

  onSubmit(): void {
    if(this.form.invalid) return;
    this.submitting = true;
    this.error = '';
    const formData = this.form.value
    this.api.createTemplate(formData).subscribe({
      next: (template) => {
        this.router.navigate(['/templates', template.id]);
      },
      error: (err) => {
        this.error = "Erreur lors de la creation";
        this.submitting = false;
        console.error(err);
      }
    })
  }

}
