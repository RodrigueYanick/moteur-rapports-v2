import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { RouterLink } from "@angular/router";
import { Template } from '../../models/template.model';
import { TemplateApiService } from '../../services/template-api';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-template-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './template-list.html',
  styleUrls: ['./template-list.scss'],
})
export class TemplateList implements OnInit {
  loading = false;
  error = '';
  templates: Template[] = [];
  rawResponse: string | null = null;

  constructor( private api: TemplateApiService, private cdr: ChangeDetectorRef ) {}

  ngOnInit(): void{
    this.loadTemplates();
  }

  loadTemplates(): void {
    this.loading = true;
    this.api.getTemplates().subscribe({
      next: (data) => {
        console.log('[TemplateList] templates loaded', data);
        this.rawResponse = JSON.stringify(data, null, 2);
        this.templates = Array.isArray(data) ? data : [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = 'Impossible de charger les modeles. Verifier que le backend est bien lancer';
        this.loading = false;
        console.error(err);
        this.cdr.detectChanges();
      }
    });
  }

}
