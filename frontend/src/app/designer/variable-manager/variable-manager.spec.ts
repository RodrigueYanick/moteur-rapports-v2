import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VariableManager } from './variable-manager';

describe('VariableManager', () => {
  let component: VariableManager;
  let fixture: ComponentFixture<VariableManager>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VariableManager]
    })
    .compileComponents();

    fixture = TestBed.createComponent(VariableManager);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
