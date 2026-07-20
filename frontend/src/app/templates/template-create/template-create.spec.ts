import { ComponentFixture, TestBed } from '@angular/core/testing';

import { TemplateCreate } from './template-create';

describe('TemplateCreate', () => {
  let component: TemplateCreate;
  let fixture: ComponentFixture<TemplateCreate>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TemplateCreate]
    })
    .compileComponents();

    fixture = TestBed.createComponent(TemplateCreate);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
