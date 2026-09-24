import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { TemplateLibrary } from './template-library';
import { TemplateApiService } from '../../services/template-api';

describe('TemplateLibrary', () => {
  let component: TemplateLibrary;
  let fixture: ComponentFixture<TemplateLibrary>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TemplateLibrary],
      providers: [
        TemplateApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TemplateLibrary);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
