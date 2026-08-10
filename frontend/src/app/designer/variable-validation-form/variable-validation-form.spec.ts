import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VariableValidationForm } from './variable-validation-form';

describe('VariableValidationForm', () => {
  let component: VariableValidationForm;
  let fixture: ComponentFixture<VariableValidationForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VariableValidationForm],
    }).compileComponents();

    fixture = TestBed.createComponent(VariableValidationForm);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
