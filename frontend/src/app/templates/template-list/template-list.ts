import { Component, OnInit } from '@angular/core';
import { CommonModule } from '../../../../node_modules/@angular/common/types/_common_module-chunk';
import { RouterLink } from "@angular/router";
import { Template } from '../../models/template.model';
import { TemplateApiService } from '../../services/template-api';

@Component({
  selector: 'app-template-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './template-list.html',
  styleUrl: './template-list.scss',
})
export class TemplateList implements OnInit {
  loading = false;
  error = '';
  templates: Template[] = [];

  constructor( private api: TemplateApiService ) {}

  ngOnInit(): void{
    this.loading = true;
    this.api.getTemplates().subscribe({
      next: (data) => {
        this.templates = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Impossible de charger les modeles. Verifier que le backend est bien lancer';
        this.loading = false;
        console.error(err);
      }
    });
  }

}
