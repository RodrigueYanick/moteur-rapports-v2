import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CodeEntrepriseModal } from './code-entreprise-modal';

describe('CodeEntrepriseModal', () => {
  let component: CodeEntrepriseModal;
  let fixture: ComponentFixture<CodeEntrepriseModal>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CodeEntrepriseModal],
    }).compileComponents();

    fixture = TestBed.createComponent(CodeEntrepriseModal);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
