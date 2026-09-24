import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { TemplateList } from './template-list';
import { TemplateApiService } from '../../services/template-api';

describe('TemplateList', () => {
  let component: TemplateList;
  let fixture: ComponentFixture<TemplateList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TemplateList],
      providers: [
        TemplateApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TemplateList);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
