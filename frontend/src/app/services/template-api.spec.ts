import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TemplateApiService } from './template-api';
import { Template } from '../models/template.model';

describe('TemplateApiService', () => {
  let service: TemplateApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TemplateApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(TemplateApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('devrait être instancié correctement', () => {
    expect(service).toBeTruthy();
  });

  it('devrait récupérer la liste des templates via GET /api/templates', () => {
    const mockTemplates: Template[] = [
      {
        id: 'tpl-1',
        nom: 'Facture Client',
        description: 'Modèle standard',
        statut: 'PUBLIE',
        version: 1,
        categorie: 'VENTES',
        formatPapier: 'A4',
        modePagination: 'FIXED',
        dateCreation: '2026-01-01',
        dateModification: '2026-01-02',
      },
    ];

    service.getTemplates('ALL', 'Facture').subscribe((templates) => {
      expect(templates.length).toBe(1);
      expect(templates[0].nom).toBe('Facture Client');
    });

    const req = httpTesting.expectOne('/api/templates?visibilite=ALL&q=Facture');
    expect(req.request.method).toBe('GET');
    req.flush(mockTemplates);
  });

  it('devrait créer un template via POST /api/templates', () => {
    const newTemplatePayload = { nom: 'Nouveau Devis', categorie: 'VENTES' };
    const createdTemplate: Template = {
      id: 'tpl-2',
      nom: 'Nouveau Devis',
      description: '',
      statut: 'BROUILLON',
      version: 1,
      categorie: 'VENTES',
      formatPapier: 'A4',
      modePagination: 'FIXED',
      dateCreation: '2026-01-01',
      dateModification: '2026-01-01',
    };

    service.createTemplate(newTemplatePayload).subscribe((template) => {
      expect(template.id).toBe('tpl-2');
      expect(template.nom).toBe('Nouveau Devis');
    });

    const req = httpTesting.expectOne('/api/templates');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(newTemplatePayload);
    req.flush(createdTemplate);
  });

  it('devrait propager une erreur HTTP 500 lors de la récupération', () => {
    let errorMessage = '';

    service.getTemplate('unknown-id').subscribe({
      next: () => {},
      error: (err) => {
        errorMessage = err.statusText;
        expect(err.status).toBe(500);
      },
    });

    const req = httpTesting.expectOne('/api/templates/unknown-id');
    req.flush({ message: 'Internal Server Error' }, { status: 500, statusText: 'Internal Server Error' });
    expect(errorMessage).toBe('Internal Server Error');
  });
});
