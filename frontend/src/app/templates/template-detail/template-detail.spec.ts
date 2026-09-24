import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { TemplateDetail } from './template-detail';
import { TemplateApiService } from '../../services/template-api';

describe('TemplateDetail', () => {
  let component: TemplateDetail;
  let fixture: ComponentFixture<TemplateDetail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TemplateDetail],
      providers: [
        TemplateApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: convertToParamMap({ id: '1' }) },
            paramMap: of(convertToParamMap({ id: '1' })),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TemplateDetail);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
