import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TemplateLibrary } from './template-library';

describe('TemplateLibrary', () => {
  let component: TemplateLibrary;
  let fixture: ComponentFixture<TemplateLibrary>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TemplateLibrary],
    }).compileComponents();

    fixture = TestBed.createComponent(TemplateLibrary);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
